package gov.nih.nci.hpc.jms.impl;

import javax.jms.Queue;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.message.HpcMessageQueue;
import gov.nih.nci.hpc.domain.message.HpcTaskMessage;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.jms.HpcJmsQueueSender;

import org.apache.activemq.ScheduledMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

public class HpcJmsQueueSenderImpl implements HpcJmsQueueSender {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	private JmsTemplate jmsTemplate;
	private Queue queue;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// Scheduled delay in milliseconds
	@Value("${hpc.jms.scheduled.delay}")
	private Long scheduledDelay = null;
	
	// True if Message Queuing is enabled.
	@Value("${hpc.jms.enableMessageQueueing}")
	private Boolean enableMessageQueueing = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for spring injection.
	 * 
	 * @param jmsTemplate jmsTemplate.
	 */
	private HpcJmsQueueSenderImpl(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;

	}

	/**
	 * Default Constructor is disabled
	 * 
	 * @throws HpcException
	 *             Constructor is disabled.
	 */
	private HpcJmsQueueSenderImpl() throws HpcException {
		throw new HpcException("Default Constructor Disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcJmsQueueSender Implementation
	// ---------------------------------------------------------------------//
	
	@Override
	public void send(HpcTaskMessage message, HpcMessageQueue queue) {
		if(enableMessageQueueing) {
			logger.debug("Sending message {}", message);
			jmsTemplate.convertAndSend(queue.value(), message);
		} else {
			logger.debug("Message Queuing is disabled {}", message);
		}
	}
	
	@Override
	public void send(HpcTaskMessage message, HpcMessageQueue queue, Boolean delay) {
		if(enableMessageQueueing) {
			if(delay) {
				logger.debug("Sending message with delay {}", message);
				jmsTemplate.convertAndSend(queue.value(), message, m -> {
					m.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, scheduledDelay);
					return m;
				});
			} else {
				logger.debug("Sending message {}", message);
				jmsTemplate.convertAndSend(queue.value(), message);
			}
		} else {
			logger.debug("Message Queuing is disabled {}", message);
		}
	}
}
