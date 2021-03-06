/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.daq.opcua.connection.soap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.axis2.AxisFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opcfoundation.xmlda.ItemValue;
import org.opcfoundation.xmlda.OPCXML_DataAccessStub;
import org.opcfoundation.xmlda.SubscribePolledRefreshReplyItemList;

import cern.c2mon.daq.opcua.connection.common.AbstractOPCUAAddress;
import cern.c2mon.daq.opcua.connection.common.impl.OPCCommunicationException;
import cern.c2mon.daq.opcua.connection.common.impl.OPCCriticalException;

/**
 * Polls the OPC server for updates to one subscription. It uses a long poll
 * mechanism. This means that the server only returns the poll if there are
 * updates or a certain time (wait time) is passed. Aditionally there is a
 * minimum waiting time (hold time).
 * <pre>
 * |------------------|----------------------------|
 *      hold time                wait time
 *  will never return      will return if updates   will return
 * </pre>
 * @author Andreas Lang
 *
 */
public class SoapLongPoll {
    /**
     * Logger of this class.
     */
    private Logger logger = LoggerFactory.getLogger(SoapLongPoll.class);

    /**
     * Exception Handler if the polling is interrupted.
     */
    private ISoapLongPollExceptionHandler exceptionHandler;

    /**
     * The Saddress to poll.
     */
    private AbstractOPCUAAddress address;

    /**
     * An executor service which serves as ThreadPool.
     */
    private static final ExecutorService EXECUTOR_SERVICE =
        Executors.newCachedThreadPool();

    /**
     * Indicates the running state of the poll.
     */
    private volatile boolean isRunning = false;

    /**
     * All registered listeners which require updates to that subscription.
     */
    private Collection<ISoapLongPollListener> listeners =
        new ConcurrentLinkedQueue<ISoapLongPollListener>();

    /**
     * The runnable for the long poll itself.
     */
    private SoapLongPollRunnable soapLongPollRunnable;

    /**
     * Hold time: Server has to wait at least that time.
     */
    private int holdTime;

    /**
     * Additional time on top of hold time to wait if there are no updates.
     */
    private int waitTime;

    /**
     * The server subscription handle.
     */
    private String serverSubscriptionHandle;

    /**
     * Creates a new soap long poll.
     * 
     * @param address The opc address to identify the poll target.
     * @param serverSubscriptionHandle The server handle which identifies the
     * Subscription to poll updates for.
     * @param holdTime The hold time. Minimum time the server has to wait to
     * return a request even if there are new updates.
     * @param waitTime The maximum time the server will wait after the hold time
     * to return a request. Even if there are no updates.
     */
    public SoapLongPoll(final AbstractOPCUAAddress address,
            final String serverSubscriptionHandle, final int holdTime, 
            final int waitTime) {
        this.address = address;
        this.serverSubscriptionHandle = serverSubscriptionHandle;
        this.holdTime = holdTime;
        this.waitTime = waitTime;
    }

    /**
     * Starts the polling of the subscription.
     */
    public synchronized void startPolling() {
        try {
            logger.debug(this + " Polling started.");
            OPCXML_DataAccessStub access = createSoapAccess();
            if (soapLongPollRunnable == null || !isRunning) {
                soapLongPollRunnable = new SoapLongPollRunnable(
                        holdTime, waitTime, serverSubscriptionHandle,
                        access) {
                            @Override
                            public void newItemValues(
                                    final SubscribePolledRefreshReplyItemList[] rItemList) {
                                notifyListeners(rItemList);
                            }

                            @Override
                            public void onError(final Throwable e) {
                                pollingThreadFailed(e);
                            }
                    
                };
                EXECUTOR_SERVICE.execute(soapLongPollRunnable);
                isRunning = true;
            }
        } catch (MalformedURLException e) {
            throw new OPCCriticalException(e);
        } catch (AxisFault e) {
            throw new OPCCommunicationException(e);
        }
    }
    
    /**
     * Creates a new acces object/http session.
     * 
     * @return The access object.
     * @throws MalformedURLException Thrown if the supplied URI is malformed.
     * @throws AxisFault Thrown if there is a problem wih Soap
     */
    private OPCXML_DataAccessStub createSoapAccess() 
            throws MalformedURLException, AxisFault {
        URL serverURL = address.getUri().toURL();
        String domain = address.getDomain();
        String user = address.getUser();
        String password = address.getPassword();
        return SoapObjectFactory.createOPCDataAccessSoapInterface(
                serverURL, domain, user, password);
    }

    /**
     * Stops the polling of the subscription.
     */
    private void stopPolling() {
        if (soapLongPollRunnable != null && isRunning) {
            soapLongPollRunnable.stop();
            isRunning = false;
        }
    }
    
    /**
     * This is called when the polling runnable fails due to whatever reason.
     * E.g. a connection loss.
     * 
     * @param e The exception which caused the failing.
     */
    private void pollingThreadFailed(final Throwable e) {
        stopPolling();
        if (exceptionHandler != null) {
            exceptionHandler.onConnectionException(e, this);
        }
    }

    /**
     * Adds a listener to this soap long poll.
     * 
     * @param listener The listener to add.
     */
    public void addListener(final ISoapLongPollListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a listener from this long poll.
     * 
     * @param listener The listener to remove.
     */
    public void removeListener(final ISoapLongPollListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Sets the Exception handler for this long poll. It will be notified if
     * the polling fails due to whatever reason.
     * 
     * @param exceptionHandler The exception handler to call if the polling
     * fails.
     */
    public void setExceptionHandler(
            final ISoapLongPollExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }
    
    /**
     * Stops polling and releases all resources.
     */
    public synchronized void release() {
        stopPolling();
        listeners.clear();
        exceptionHandler = null;
        logger.debug(this + " Poll released");
    }

    /**
     * Notifies all registered listeners about value changes contained in
     * rItemList.
     * 
     * @param rItemList List of item vlaues which have changed.
     */
    public void notifyListeners(final SubscribePolledRefreshReplyItemList[] rItemList) {
        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                logger.debug(SoapLongPoll.this + " Poll returned.");
                if (rItemList != null) {
                    for (SubscribePolledRefreshReplyItemList subscripion : rItemList) {
                        logger.debug(SoapLongPoll.this + " Number of returned items: " + subscripion.getItems().length);
                        for (ItemValue itemValue : subscripion.getItems()) {
                            String clientItemHandle = itemValue.getClientItemHandle();
                            Object value = itemValue.getValue().getText();
                            long timestamp = itemValue.getTimestamp().getTimeInMillis();
                            for (ISoapLongPollListener listener : listeners) {
                                listener.valueChanged(clientItemHandle, timestamp, value);
                            }
                        }
                    }
                }
            }
        });
    }

}
