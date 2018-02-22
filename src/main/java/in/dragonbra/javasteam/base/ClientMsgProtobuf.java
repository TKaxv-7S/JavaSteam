package in.dragonbra.javasteam.base;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.GeneratedMessageV3;
import in.dragonbra.javasteam.enums.EMsg;
import in.dragonbra.javasteam.generated.MsgHdrProtoBuf;
import in.dragonbra.javasteam.util.stream.MemoryStream;
import in.dragonbra.javasteam.util.stream.SeekOrigin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents a protobuf backed client message.
 *
 * @param <BodyType> The body type of this message.
 */
public class ClientMsgProtobuf<BodyType extends GeneratedMessageV3.Builder<BodyType>> extends AClientMsgProtobuf {

    private static final Logger logger = LogManager.getLogger(ClientMsgProtobuf.class);

    private BodyType body;

    /**
     * Initializes a new instance of the {@link ClientMsgProtobuf} class.
     * This is a client send constructor.
     *
     * @param clazz
     * @param msg   The network message type this client message represents.
     */
    public ClientMsgProtobuf(Class<? extends AbstractMessage> clazz, IPacketMsg msg) {
        this(clazz, msg, 64);
    }

    /**
     * Initializes a new instance of the {@link ClientMsgProtobuf} class.
     * This is a client send constructor.
     *
     * @param clazz
     * @param msg   The network message type this client message represents.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ClientMsgProtobuf(Class<? extends AbstractMessage> clazz, IPacketMsg msg, int payloadReserve) {
        this(clazz, msg.getMsgType(), payloadReserve);
    }

    /**
     * Initializes a new instance of the {@link ClientMsgProtobuf} class.
     * This is a client send constructor.
     *
     * @param clazz
     * @param eMsg  The network message type this client message represents.
     */
    public ClientMsgProtobuf(Class<? extends AbstractMessage> clazz, EMsg eMsg) {
        this(clazz, eMsg, 64);
    }

    /**
     * Initializes a new instance of the {@link ClientMsgProtobuf} class.
     * This is a client send constructor.
     *
     * @param clazz
     * @param eMsg           The network message type this client message represents.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ClientMsgProtobuf(Class<? extends AbstractMessage> clazz, EMsg eMsg, int payloadReserve) {
        super(payloadReserve);

        try {
            final Method m = clazz.getMethod("newBuilder");
            body = (BodyType) m.invoke(null);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            logger.debug(e);
        }

        getHeader().setEMsg(eMsg);
    }

    /**
     * Initializes a new instance of the {@link ClientMsgProtobuf} class.
     * This is a reply constructor.
     *
     * @param clazz
     * @param eMsg  The network message type this client message represents.
     * @param msg   The message that this instance is a reply for.
     */
    public ClientMsgProtobuf(Class<? extends AbstractMessage> clazz, EMsg eMsg, MsgBase<MsgHdrProtoBuf> msg) {
        this(clazz, eMsg, msg, 64);
    }

    /**
     * Initializes a new instance of the {@link ClientMsgProtobuf} class.
     * This is a reply constructor.
     *
     * @param clazz
     * @param eMsg           The network message type this client message represents.
     * @param msg            The message that this instance is a reply for.
     * @param payloadReserve The number of bytes to initialize the payload capacity to.
     */
    public ClientMsgProtobuf(Class<? extends AbstractMessage> clazz, EMsg eMsg, MsgBase<MsgHdrProtoBuf> msg, int payloadReserve) {
        this(clazz, eMsg, payloadReserve);
        // our target is where the message came from
        getHeader().getProto().setJobidTarget(msg.getHeader().getProto().getJobidSource());
    }

    /**
     * Gets the body structure of this message.
     */
    public BodyType getBody() {
        return body;
    }

    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(0);
        DataOutputStream dos = new DataOutputStream(baos);

        dos.write(body.build().toByteArray());

        getHeader().serialize(baos);
        dos.write(payload.getBuffer());
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] data) throws IOException {
        MemoryStream ms = new MemoryStream(data);

        getHeader().deserialize(ms);

        payload.write(data, (int) ms.getPosition(), ms.available());
        payload.seek(0, SeekOrigin.BEGIN);
    }
}