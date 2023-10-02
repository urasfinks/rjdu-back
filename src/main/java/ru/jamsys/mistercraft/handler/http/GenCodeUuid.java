package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.JsonHttpResponse;
import ru.jamsys.Util;
import ru.jamsys.cache.TokenManager;
import ru.jamsys.mistercraft.UserSessionInfo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GenCodeUuid implements HttpHandler {

    static AtomicBoolean available = new AtomicBoolean(true);

    static TokenManager<Integer, String> codeManager = new TokenManager<>(); // 12356: dataUuid
    static TokenManager<String, String> dataManager = new TokenManager<>(); // dataUuid: deviceUuid

    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {

        Map<String, Object> req = (Map<String, Object>) jRet.getData().get("request");
        final String uuid = req.containsKey("uuid") ? (String) req.get("uuid") : null;
        dataManager.add(uuid, userSessionInfo.getDeviceUuid(), 60 * 60 * 24 * 1000);

        sync(jsonHttpResponse -> {
            int maxCount = 5000;
            int count = 0;
            while (true) {
                Integer random = Util.random(100000, 999999);
                if (codeManager.add(random, uuid, 60 * 5 * 1000)) {
                    jsonHttpResponse.addData("code", random);
                    jsonHttpResponse.addData("uuid", uuid);
                    break;
                }
                if (count++ > maxCount) {
                    break;
                }
            }
        }, jRet);
    }

    //Синхронизация получения кода в многопользовательском режиме
    private void sync(Consumer<JsonHttpResponse> cons, JsonHttpResponse jRet) {
        int count = 0;
        int maxCount = 70; //7 sec
        while (true) {
            if (available.compareAndSet(true, false)) {
                try {
                    cons.accept(jRet);
                } catch (Exception e) {
                    jRet.addException(e);
                }
                available.set(true);
                break;
            }
            Util.sleepMillis(100);
            if (count++ > maxCount) {
                break;
            }
        }
    }
}
