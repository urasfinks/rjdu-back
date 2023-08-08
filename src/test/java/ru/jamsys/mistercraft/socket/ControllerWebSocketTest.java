package ru.jamsys.mistercraft.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.App;
import ru.jamsys.jdbc.template.Executor;
import ru.jamsys.mistercraft.jt.Data;


import java.util.Map;

class ControllerWebSocketTest {

    @BeforeAll
    static void beforeAll() throws Exception {
        String[] args = new String[]{};
        App.main(args);
    }

    String getSubscribe() {
        return """
                {
                    "request": {
                        "handler": "SUBSCRIBE",
                        "uuid_data": "test"
                    }
                }
                """;
    }

    String getUnsubscribe() {
        return """
                {
                    "request": {
                        "handler": "UNSUBSCRIBE",
                        "uuid_data": "test"
                    }
                }
                """;
    }

    String getBroadCast() {
        return """
                {
                    "request": {
                        "handler": "BROADCAST",
                        "uuid_data": "test",
                        "data": {
                            "message": "Hello world"
                        }
                    }
                }
                """;
    }

    @Test
    void onRead() {
        Assertions.assertTrue(validate(getSubscribe()), "#1");
        Assertions.assertTrue(validate(getUnsubscribe()), "#2");
        Assertions.assertTrue(validate(getBroadCast()), "#3");
    }

    @Test
    void lock() throws Exception {
        Executor executor = App.jdbcTemplate.getExecutor(App.postgresqlPoolName);

        Map<String, Object> arguments = App.jdbcTemplate.createArguments();
        executor.execute(Data.LOCK, arguments);
        Thread.sleep(10000);
        executor.execute(Data.UNLOCK, arguments);
        Thread.sleep(10000);
        executor.close();
    }

    private boolean validate(String data) {
        RequestMessageReader handler = App.context.getBean(RequestMessageReader.class);
        RequestMessage message = new RequestMessage();
        message.setBody(data);
        return handler.onRead(message);
    }

}