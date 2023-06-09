package ru.jamsys.mistercraft.socket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.jamsys.Util;
import ru.jamsys.UtilFileResource;
import ru.jamsys.UtilJson;
import ru.jamsys.WrapJsonToObject;
import ru.jamsys.component.Broker;
import ru.jamsys.component.JsonSchema;
import ru.jamsys.component.ThreadBalancerFactory;
import ru.jamsys.message.Message;
import ru.jamsys.mistercraft.ControllerWebSocket;
import ru.jamsys.mistercraft.handler.socket.HandlerMethod;
import ru.jamsys.thread.balancer.ThreadBalancerImpl;

import java.util.Map;

@Component
public class RequestMessageReader {

    String schemaSocketRequest;

    JsonSchema jsonSchema;
    Broker broker;
    ThreadBalancerFactory threadBalancerFactory;

    public RequestMessageReader() {
        try {
            schemaSocketRequest = UtilFileResource.getAsString("schema/socket/ProtocolRequest.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Autowired
    @SuppressWarnings("unused")
    public void setJsonSchema(JsonSchema jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    @Autowired
    @SuppressWarnings("unused")
    public void setThreadBalancerFactory(ThreadBalancerFactory threadBalancerFactory) {
        this.threadBalancerFactory = threadBalancerFactory;
    }

    @Autowired
    @SuppressWarnings("unused")
    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public void init() {
        ThreadBalancerImpl tbSocketRequestReader = threadBalancerFactory.create(ControllerWebSocket.nameSocketRequestReader, 1, 5, 10, 60000);
        tbSocketRequestReader.setSupplier(() -> broker.pollLast(RequestMessage.class));
        tbSocketRequestReader.setConsumer(this::onRead);
    }

    public boolean onRead(Message message) {
        JsonSchema.Result validate = validateRequest(message.getBody());
        if (validate.isValidate()) {

            WrapJsonToObject<Map<String, Map<String, Object>>> mapWrapJsonToObject = UtilJson.toMap(message.getBody());
            Map<String, Object> parsedJson = mapWrapJsonToObject.getObject().get("request");
            HandlerMethod handlerMethod = HandlerMethod.valueOf((String) parsedJson.get("handler"));

            Request request = new Request();
            request.setHandlerMethod(handlerMethod);
            request.setUuidData((String) parsedJson.get("uuid_data"));
            request.setRequestMessage((RequestMessage) message);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) parsedJson.get("data");
            request.setData(data);
            request.exec();
            //Util.logConsole("onRead: " + message.getBody() + "; Request: " + request.toString());
        } else {
            Util.logConsole("onRead: " + message.getBody() + "; Exception: " + validate.getError());
            return false;
        }
        return true;
    }


    public JsonSchema.Result validateRequest(String data) {
        return jsonSchema.validate(data, schemaSocketRequest);
    }



}
