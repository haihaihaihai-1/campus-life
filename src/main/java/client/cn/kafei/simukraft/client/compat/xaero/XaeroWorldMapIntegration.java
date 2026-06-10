package client.cn.kafei.simukraft.client.compat.xaero;

import common.cn.kafei.simukraft.SimuKraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xaero.map.highlight.AbstractHighlighter;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class XaeroWorldMapIntegration {
    private static volatile boolean registeredOnce;

    private XaeroWorldMapIntegration() {
    }

    /** registerHighlighter: 向 Xaero World Map 注册城市区块高亮器。 */
    public static void registerHighlighter(List<AbstractHighlighter> highlighters) {
        if (highlighters == null || containsSimuKraftHighlighter(highlighters)) {
            return;
        }
        highlighters.add(new SimuKraftCityHighlighter());
        if (!registeredOnce) {
            registeredOnce = true;
            SimuKraft.LOGGER.info("Simukraft: Registered city chunk highlighter for Xaero's World Map.");
        }
    }

    /** containsSimuKraftHighlighter: 防止 Xaero 会话重复初始化时重复添加高亮器。 */
    private static boolean containsSimuKraftHighlighter(List<AbstractHighlighter> highlighters) {
        for (AbstractHighlighter highlighter : highlighters) {
            if (highlighter instanceof SimuKraftCityHighlighter) {
                return true;
            }
        }
        return false;
    }
}
