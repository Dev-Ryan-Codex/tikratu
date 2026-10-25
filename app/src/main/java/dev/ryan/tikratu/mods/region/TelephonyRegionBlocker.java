package dev.ryan.tikratu.mods.region;

import android.telephony.TelephonyManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes8.dex */
public class TelephonyRegionBlocker {
    private static final Map<String, String> ISO_TO_MCC = buildMccTable();
    private static boolean logged;

    private enum Field {
        COUNTRY,
        OPERATOR,
        OPERATOR_NAME
    }

    public void block(ClassLoader classLoader) {
        hookString("getSimCountryIso", Field.COUNTRY);
        hookString("getNetworkCountryIso", Field.COUNTRY);
        hookString("getSimOperator", Field.OPERATOR);
        hookString("getNetworkOperator", Field.OPERATOR);
        hookString("getSimOperatorName", Field.OPERATOR_NAME);
        hookString("getNetworkOperatorName", Field.OPERATOR_NAME);
        ModuleLog.line("(TikRatu | TelephonyRegionBlocker): hooked TelephonyManager (país/operador)");
    }

    private void hookString(String method, final Field field) {
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, method, new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.region.TelephonyRegionBlocker.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_SIM_REGION, false)) {
                        String code = RuntimeSettings.getString(Prefs.KEY_REGION_CODE, "").trim().toLowerCase();
                        if (code.isEmpty()) {
                            return;
                        }
                        String mcc = (String) TelephonyRegionBlocker.ISO_TO_MCC.get(code);
                        switch (field) {
                            case COUNTRY:
                                param.setResult(code);
                                break;
                            case OPERATOR:
                                if (mcc != null) {
                                    param.setResult(mcc + "01");
                                }
                                break;
                            case OPERATOR_NAME:
                                if (mcc != null) {
                                    param.setResult("Carrier");
                                }
                                break;
                        }
                        if (!TelephonyRegionBlocker.logged) {
                            TelephonyRegionBlocker.logged = true;
                            ModuleLog.line("(TikRatu | TelephonyRegionBlocker): SIM spoof activo -> país=" + code + (mcc != null ? " MCC=" + mcc : " (sin MCC en tabla, solo país)"));
                        }
                    }
                }
            }});
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | TelephonyRegionBlocker): no se pudo hookear " + method + " (" + t.getMessage() + ")");
        }
    }

    private static Map<String, String> buildMccTable() {
        Map<String, String> m = new HashMap<>();
        m.put("us", "310");
        m.put("ca", "302");
        m.put("mx", "334");
        m.put("br", "724");
        m.put("ar", "722");
        m.put("cl", "730");
        m.put("co", "732");
        m.put("pe", "716");
        m.put("ve", "734");
        m.put("ec", "740");
        m.put("bo", "736");
        m.put("py", "744");
        m.put("uy", "748");
        m.put("cr", "712");
        m.put("pa", "714");
        m.put("do", "370");
        m.put("gt", "704");
        m.put("sv", "706");
        m.put("hn", "708");
        m.put("ni", "710");
        m.put("pr", "330");
        m.put("cu", "368");
        m.put("es", "214");
        m.put("fr", "208");
        m.put("de", "262");
        m.put("gb", "234");
        m.put("it", "222");
        m.put("pt", "268");
        m.put("nl", "204");
        m.put("be", "206");
        m.put("ch", "228");
        m.put("at", "232");
        m.put("se", "240");
        m.put("no", "242");
        m.put("dk", "238");
        m.put("fi", "244");
        m.put("pl", "260");
        m.put("cz", "230");
        m.put("ru", "250");
        m.put("ua", "255");
        m.put("ro", "226");
        m.put("gr", "202");
        m.put("hu", "216");
        m.put("ie", "272");
        m.put("rs", "220");
        m.put("bg", "284");
        m.put("hr", "219");
        m.put("sk", "231");
        m.put("jp", "440");
        m.put("kr", "450");
        m.put("cn", "460");
        m.put("in", "404");
        m.put("id", "510");
        m.put("ph", "515");
        m.put("th", "520");
        m.put("vn", "452");
        m.put("my", "502");
        m.put("sg", "525");
        m.put("tw", "466");
        m.put("hk", "454");
        m.put("pk", "410");
        m.put("bd", "470");
        m.put("lk", "413");
        m.put("np", "429");
        m.put("kz", "401");
        m.put("kh", "456");
        m.put("mm", "414");
        m.put("sa", "420");
        m.put("ae", "424");
        m.put("il", "425");
        m.put("tr", "286");
        m.put("ir", "432");
        m.put("iq", "418");
        m.put("qa", "427");
        m.put("kw", "419");
        m.put("jo", "416");
        m.put("lb", "415");
        m.put("za", "655");
        m.put("ng", "621");
        m.put("eg", "602");
        m.put("ke", "639");
        m.put("ma", "604");
        m.put("dz", "603");
        m.put("tn", "605");
        m.put("gh", "620");
        m.put("et", "636");
        m.put("ao", "631");
        m.put("au", "505");
        m.put("nz", "530");
        return m;
    }
}
