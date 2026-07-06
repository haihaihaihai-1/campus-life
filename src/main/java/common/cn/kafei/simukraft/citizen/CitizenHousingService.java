package common.cn.kafei.simukraft.citizen;

import common.cn.kafei.simukraft.city.poi.CityPoiData;
import common.cn.kafei.simukraft.city.poi.CityPoiManager;
import common.cn.kafei.simukraft.city.poi.CityPoiType;
import common.cn.kafei.simukraft.city.group.CityGroupMessageService;
import common.cn.kafei.simukraft.building.PlacedBuildingService;
import common.cn.kafei.simukraft.building.PlacedBuildingRecord;
import common.cn.kafei.simukraft.building.BuildingUnitInstance;
import common.cn.kafei.simukraft.citizen.family.FamilyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("null")
public final class CitizenHousingService {
    private CitizenHousingService() {
    }

    public static int fillVacantHomes(ServerLevel level, UUID cityId) {
        return fillVacantHomes(level, cityId, Integer.MAX_VALUE);
    }

    public static int fillVacantHomes(ServerLevel level, UUID cityId, int maxAssignments) {
        if (level == null || cityId == null || maxAssignments <= 0) return 0;

        CityPoiManager poiManager = CityPoiManager.get(level);
        Set<UUID> assigned = new java.util.HashSet<>();

        // 阶段一：家庭整体分配到同一住宅户
        int familyAssigned = fillFamilyUnits(level, cityId, poiManager, assigned);

        // 阶段二：Phase 1 已更新内存中 homeId，重新计算 occupied 包含 Phase 1 分配结果，避免双重分配
        Set<UUID> occupied = occupiedPoiIds(CitizenManager.get(level), cityId, poiManager);
        Set<UUID> blockedByOccupied = vacantPoiIdsInPartiallyOccupied(level, cityId, poiManager, occupied);
        List<CityPoiData> vacantHomes = vacantHomes(level, cityId, java.util.Collections.emptySet()).stream()
                .filter(poi -> !blockedByOccupied.contains(poi.poiId()))
                .toList();
        if (vacantHomes.isEmpty()) return familyAssigned;

        List<CitizenData> homelessCitizens = CitizenManager.get(level).allCitizens().stream()
                .filter(c -> !c.dead())
                .filter(c -> cityId.equals(c.cityId()) && !hasValidHome(poiManager, cityId, c.homeId()))
                .filter(c -> !assigned.contains(c.uuid()))
                .sorted(Comparator.comparing(CitizenData::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int limit = Math.min(Math.min(vacantHomes.size(), homelessCitizens.size()), Math.max(0, maxAssignments - familyAssigned));
        for (int i = 0; i < limit; i++) {
            CitizenService.setHome(level, homelessCitizens.get(i).uuid(), vacantHomes.get(i).poiId());
        }
        return familyAssigned + limit;
    }

    public static int spawnCitizensForVacantHomes(ServerLevel level, UUID cityId, BlockPos spawnPos, int maxSpawns) {
        if (level == null || cityId == null || spawnPos == null || maxSpawns <= 0) {
            return 0;
        }
        CityPoiManager spawnPoiManager = CityPoiManager.get(level);
        Set<UUID> spawnOccupied = occupiedPoiIds(CitizenManager.get(level), cityId, spawnPoiManager);
        Set<UUID> spawnBlocked = vacantPoiIdsInPartiallyOccupied(level, cityId, spawnPoiManager, spawnOccupied);
        java.util.List<CityPoiData> vacantHomes = new java.util.ArrayList<>(vacantHomes(level, cityId).stream()
                .filter(poi -> !spawnBlocked.contains(poi.poiId()))
                .toList());
        int spawned = 0;
        while (spawned < maxSpawns && !vacantHomes.isEmpty()) {
            CityPoiData home = vacantHomes.remove(0);
            Vec3 spawnTarget = resolveNewResidentSpawnTarget(level, home, spawnPos);
            var citizen = CitizenService.spawnCitizen(level, spawnTarget, cityId, true);
            if (citizen.isEmpty()) {
                break;
            }
            CitizenData data = CitizenService.ensureCitizen(level, citizen.get());
            if (data != null) {
                CitizenService.setHome(level, data.uuid(), home.poiId());
                notifyNewResident(level, cityId, data);
                spawned++;
            }
        }
        return spawned;
    }

    private static int fillFamilyUnits(ServerLevel level, UUID cityId,
            CityPoiManager poiManager, Set<UUID> assignedCitizens) {
        FamilyManager familyManager = FamilyManager.get(level);
        CitizenManager citizenManager = CitizenManager.get(level);
        Set<UUID> occupiedPoiIds = occupiedPoiIds(citizenManager, cityId, poiManager);
        int count = 0;

        for (var family : familyManager.getCityFamilies(cityId)) {
            // 收集家庭中无家可归的成员
            List<UUID> homeless = new java.util.ArrayList<>();
            addIfHomeless(family.husbandId(), citizenManager, poiManager, cityId, assignedCitizens, homeless);
            addIfHomeless(family.wifeId(),   citizenManager, poiManager, cityId, assignedCitizens, homeless);
            for (UUID childId : family.childIds()) {
                addIfHomeless(childId, citizenManager, poiManager, cityId, assignedCitizens, homeless);
            }
            if (homeless.isEmpty()) continue;

            // 优先：把无家成员安置到家庭现有住所（如父母已住，孩子加入同一栋）
            List<UUID> vacantPoiIds = findVacantBedsInFamilyHome(level, family, citizenManager, poiManager, occupiedPoiIds);
            if (vacantPoiIds.isEmpty()) {
                // 退路：找完全空置的建筑/户型
                vacantPoiIds = findPoiIdsForFamily(level, cityId, poiManager, occupiedPoiIds, homeless.size());
            }
            if (vacantPoiIds.isEmpty()) continue;

            // 分配
            int slot = 0;
            for (UUID poiId : vacantPoiIds) {
                if (slot >= homeless.size()) break;
                if (occupiedPoiIds.contains(poiId)) continue;
                CitizenService.setHome(level, homeless.get(slot), poiId);
                assignedCitizens.add(homeless.get(slot));
                occupiedPoiIds.add(poiId);
                slot++;
                count++;
            }
        }
        return count;
    }

    private static void addIfHomeless(UUID citizenId, CitizenManager manager,
            CityPoiManager poiManager, UUID cityId, Set<UUID> alreadyAssigned, List<UUID> result) {
        if (citizenId == null || alreadyAssigned.contains(citizenId)) return;
        CitizenData c = manager.getCitizen(citizenId).orElse(null);
        if (c == null || c.dead()) return;
        if (!cityId.equals(c.cityId())) return;
        if (!hasValidHome(poiManager, cityId, c.homeId())) result.add(citizenId);
    }

    /** findVacantBedsInFamilyHome: 查找家庭现有住所内的空床，供无家成员（如新生儿）加入。 */
    private static List<UUID> findVacantBedsInFamilyHome(ServerLevel level, common.cn.kafei.simukraft.citizen.family.FamilyData family,
            CitizenManager citizenManager, CityPoiManager poiManager, Set<UUID> occupiedPoiIds) {
        // 找任意已有家的家庭成员
        UUID housedHomeId = null;
        for (UUID memberId : membersOf(family)) {
            CitizenData c = citizenManager.getCitizen(memberId).orElse(null);
            if (c != null && !c.dead() && c.homeId() != null && occupiedPoiIds.contains(c.homeId())) {
                housedHomeId = c.homeId();
                break;
            }
        }
        if (housedHomeId == null) return List.of();

        PlacedBuildingRecord building = PlacedBuildingService.findByPoi(level, housedHomeId);
        if (building == null) return List.of();

        // 若建筑有户型定义，限定在 housedHomeId 所在的同一户型内收集空床
        List<UUID> vacant = new java.util.ArrayList<>();
        if (!building.unitInstances().isEmpty()) {
            for (BuildingUnitInstance unit : building.unitInstances()) {
                if (!unit.poiIds().contains(housedHomeId)) continue;
                for (UUID poiId : unit.poiIds()) {
                    if (!occupiedPoiIds.contains(poiId)) vacant.add(poiId);
                }
                break;
            }
        } else {
            for (var inst : building.poiInstances()) {
                if (inst.poiType() != CityPoiType.RESIDENTIAL) continue;
                var poi = poiManager.getPoiAt(inst.worldPos());
                if (poi == null || !poi.active()) continue;
                if (!occupiedPoiIds.contains(poi.poiId())) vacant.add(poi.poiId());
            }
        }
        return vacant;
    }

    private static List<UUID> membersOf(common.cn.kafei.simukraft.citizen.family.FamilyData family) {
        List<UUID> members = new java.util.ArrayList<>();
        if (family.husbandId() != null) members.add(family.husbandId());
        if (family.wifeId() != null) members.add(family.wifeId());
        members.addAll(family.childIds());
        return members;
    }

    // 返回目标户的 POI 列表：优先找 BuildingUnitInstance，无则整栋楼视为一户
    // 有正式户或默认整楼：必须完全空置才允许新家庭入住（一户一家庭）
    private static List<UUID> findPoiIdsForFamily(ServerLevel level, UUID cityId,
            CityPoiManager poiManager, Set<UUID> occupiedPoiIds, int needed) {
        // 收集所有合法候选，按床位数降序，优先把大房子分给家庭
        record Candidate(List<UUID> poiIds) {}
        List<Candidate> candidates = new java.util.ArrayList<>();

        for (var building : PlacedBuildingService.getBuildings(level)) {
            if (!cityId.equals(building.cityId())) continue;
            if (!building.unitInstances().isEmpty()) {
                for (BuildingUnitInstance unit : building.unitInstances()) {
                    if (unit.poiIds().stream().anyMatch(occupiedPoiIds::contains)) continue;
                    if (unit.poiIds().size() >= needed) candidates.add(new Candidate(unit.poiIds()));
                }
            } else {
                List<UUID> allResidential = buildingResidentialPoiIds(building, poiManager);
                if (allResidential.stream().anyMatch(occupiedPoiIds::contains)) continue;
                List<UUID> vacant = allResidential.stream()
                        .filter(id -> !occupiedPoiIds.contains(id)).toList();
                if (vacant.size() >= needed) candidates.add(new Candidate(vacant));
            }
        }
        if (candidates.isEmpty()) return List.of();
        candidates.sort(Comparator.comparingInt((Candidate c) -> c.poiIds().size()).reversed());
        return candidates.get(0).poiIds();
    }

    /** vacantPoiIdsInPartiallyOccupied: 返回所有"已有住户的建筑/户型中剩余空床"的 POI ID 集合，用于阻止陌生人入住。 */
    private static Set<UUID> vacantPoiIdsInPartiallyOccupied(ServerLevel level, UUID cityId,
            CityPoiManager poiManager, Set<UUID> occupiedPoiIds) {
        Set<UUID> blocked = new java.util.HashSet<>();
        for (var building : common.cn.kafei.simukraft.building.PlacedBuildingService.getBuildings(level)) {
            if (!cityId.equals(building.cityId())) continue;
            if (!building.unitInstances().isEmpty()) {
                for (common.cn.kafei.simukraft.building.BuildingUnitInstance unit : building.unitInstances()) {
                    if (unit.poiIds().stream().anyMatch(occupiedPoiIds::contains)) {
                        blocked.addAll(unit.poiIds());
                    }
                }
            } else {
                List<UUID> residentialIds = buildingResidentialPoiIds(building, poiManager);
                if (residentialIds.stream().anyMatch(occupiedPoiIds::contains)) {
                    blocked.addAll(residentialIds);
                }
            }
        }
        return blocked;
    }

    private static List<UUID> buildingResidentialPoiIds(common.cn.kafei.simukraft.building.PlacedBuildingRecord building, CityPoiManager poiManager) {
        List<UUID> result = new java.util.ArrayList<>();
        for (var inst : building.poiInstances()) {
            if (inst.poiType() != CityPoiType.RESIDENTIAL) continue;
            var poi = poiManager.getPoiAt(inst.worldPos());
            if (poi != null && poi.active()) result.add(poi.poiId());
        }
        return result;
    }

    private static Set<UUID> occupiedPoiIds(CitizenManager manager, UUID cityId, CityPoiManager poiManager) {
        return manager.allCitizens().stream()
                .filter(c -> !c.dead() && cityId.equals(c.cityId()))
                .filter(c -> hasValidHome(poiManager, cityId, c.homeId()))
                .map(CitizenData::homeId)
                .collect(Collectors.toSet());
    }

    private static List<CityPoiData> vacantHomes(ServerLevel level, UUID cityId, Set<UUID> excludedCitizenIds) {
        CityPoiManager poiManager = CityPoiManager.get(level);
        Set<UUID> occupied = CitizenManager.get(level).allCitizens().stream()
                .filter(c -> !c.dead() && cityId.equals(c.cityId()) && !excludedCitizenIds.contains(c.uuid()))
                .filter(c -> hasValidHome(poiManager, cityId, c.homeId()))
                .map(CitizenData::homeId)
                .collect(Collectors.toSet());
        return poiManager.getCityPois(cityId, CityPoiType.RESIDENTIAL).stream()
                .filter(CityPoiData::active)
                .filter(poi -> !occupied.contains(poi.poiId()))
                .sorted(Comparator.comparing(poi -> poi.pos().asLong()))
                .toList();
    }

    /** notifyNewResident: 新市民成功入住后通知城市在线成员。 */
    private static void notifyNewResident(ServerLevel level, UUID cityId, CitizenData data) {
        if (data == null) {
            return;
        }
        CityGroupMessageService.successToCity(level, cityId, Component.translatable("message.simukraft.citizen.joined_city", data.name()));
    }

    public static int vacantHomeCount(ServerLevel level, UUID cityId) {
        return vacantHomes(level, cityId).size();
    }

    private static Vec3 resolveNewResidentSpawnTarget(ServerLevel level, CityPoiData home, BlockPos fallbackPos) {
        if (home != null) {
            Vec3 homeTarget = CitizenHomeRestService.resolveHomeTarget(level, home.pos());
            if (homeTarget != null) {
                return homeTarget;
            }
        }
        return Vec3.atBottomCenterOf(fallbackPos).add(0.0D, 1.0D, 0.0D);
    }

    private static List<CityPoiData> vacantHomes(ServerLevel level, UUID cityId) {
        CityPoiManager poiManager = CityPoiManager.get(level);
        Set<UUID> occupiedHomes = CitizenManager.get(level).allCitizens().stream()
                .filter(citizen -> !citizen.dead())
                .filter(citizen -> cityId.equals(citizen.cityId()) && hasValidHome(poiManager, cityId, citizen.homeId()))
                .map(CitizenData::homeId)
                .collect(Collectors.toSet());
        return poiManager.getCityPois(cityId, CityPoiType.RESIDENTIAL).stream()
                .filter(CityPoiData::active)
                .filter(poi -> !occupiedHomes.contains(poi.poiId()))
                .sorted(Comparator.comparing(poi -> poi.pos().asLong()))
                .toList();
    }

    private static boolean hasValidHome(CityPoiManager poiManager, UUID cityId, UUID homeId) {
        if (poiManager == null || cityId == null || homeId == null) {
            return false;
        }
        CityPoiData home = poiManager.getPoi(homeId);
        return home != null && home.active() && home.type() == CityPoiType.RESIDENTIAL && cityId.equals(home.cityId());
    }
}
