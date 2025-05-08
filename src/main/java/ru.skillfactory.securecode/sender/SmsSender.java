package ru.skillfactory.securecode.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.SubmitSM;

import java.util.Properties;

public class SmsSender implements OtpSender {
    private static final Logger logger = LoggerFactory.getLogger(SmsSender.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;
    private final boolean enabled;

    public SmsSender() {
        logger.debug("Initializing SmsSender");
        Properties config = loadConfig();
        this.host = config.getProperty("smpp.host");
        this.port = Integer.parseInt(config.getProperty("smpp.port"));
        this.systemId = config.getProperty("smpp.system_id");
        this.password = config.getProperty("smpp.password");
        this.systemType = config.getProperty("smpp.system_type");
        this.sourceAddress = config.getProperty("smpp.source_addr");
        this.enabled = Boolean.parseBoolean(config.getProperty("enabled"));

        logger.info("SmsSender initialized with host: {}, port: {}, sourceAddr: {}, enabled: {}", host, port, sourceAddress, enabled);
    }

    private Properties loadConfig() {
        try {
            logger.debug("Loading SMS configuration from 'sms.properties'");
            Properties props = new Properties();
            props.load(SmsSender.class.getClassLoader().getResourceAsStream("sms.properties"));
            logger.info("SMS configuration loaded successfully");
            return props;
        } catch (Exception e) {
            logger.error("Failed to load SMS configuration", e);
            throw new RuntimeException("Failed to load SMS configuration", e);
        }
    }

    @Override
    public void sendOtp(String destination, String code) {
        if (!enabled) {
            logger.warn("SMS sending is disabled. Skipping sending OTP to {}", destination);
            return;
        }

        logger.debug("Attempting to send OTP to {}", destination);

        try {
            TCPIPConnection connection = new TCPIPConnection(host, port);
            Session session = new Session(connection);

            BindTransmitter bindRequest = new BindTransmitter();
            bindRequest.setSystemId(systemId);
            bindRequest.setPassword(password);
            bindRequest.setSystemType(systemType);
            bindRequest.setInterfaceVersion((byte) 0x34);
            bindRequest.setAddressRange(sourceAddress);

            BindResponse bindResponse = session.bind(bindRequest);

            if (bindResponse.getCommandStatus() != 0) {
                logger.error("SMPP bind failed with status: {}", bindResponse.getCommandStatus());
                throw new RuntimeException("Bind failed: " + bindResponse.getCommandStatus());
            }

            SubmitSM submit = new SubmitSM();
            submit.setSourceAddr(sourceAddress);
            submit.setDestAddr(destination);
            submit.setShortMessage("Your code: " + code);

            session.submit(submit);
            logger.info("OTP SMS sent successfully to {}", destination);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", destination, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }
}
