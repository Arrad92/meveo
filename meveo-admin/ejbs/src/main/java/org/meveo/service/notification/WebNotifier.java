package org.meveo.service.notification;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.elresolver.ELException;
import org.meveo.model.notification.NotificationHistoryStatusEnum;
import org.meveo.model.notification.WebNotification;
import org.meveo.model.notification.WebNotificationIdStrategyEnum;
import org.meveo.security.MeveoUser;
import org.meveo.security.keycloak.CurrentUserProvider;
import org.meveo.service.base.MeveoValueExpressionWrapper;
import org.meveo.service.communication.impl.SseManager;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Stateless
class WebNotifier {

    @Inject
    private Logger log;

    @Inject
    private SseManager sseManager;

    @Inject
    private NotificationHistoryService notificationHistoryService;

    @Inject
    private CurrentUserProvider currentUserProvider;

    private String evaluate(String expression, Object entityOrEvent, Map<String, Object> context) throws ELException {
        HashMap<Object, Object> userMap = new HashMap<>();
        userMap.put("event", entityOrEvent);
        userMap.put("context", context);
        return (String) MeveoValueExpressionWrapper.evaluateExpression(expression, userMap, String.class);
    }

    void sendMessage(WebNotification webNotif, Object entityOrEvent, Map<String, Object> context, MeveoUser lastCurrentUser) {

        currentUserProvider.reestablishAuthentication(lastCurrentUser);

        Map<Object,Object> oContext = new HashMap<>(context);
        oContext.put("event",entityOrEvent);

        log.debug("WebNotification sendMessage");

        String id= UUID.randomUUID().toString();
        if(webNotif.getIdStrategy()== WebNotificationIdStrategyEnum.TIMESTAMP){
            id = ""+System.currentTimeMillis();
        }
        try {
            if(webNotif.getDataEL()!=null && !webNotif.getDataEL().isEmpty()) {
                String dataEL_evaluated = evaluate(webNotif.getDataEL(), entityOrEvent, context);
                log.debug("Evaluated dataEL_evaluated={}", dataEL_evaluated);
                sseManager.sendMessage(id,webNotif.getCode(),webNotif.getDescription(),dataEL_evaluated,oContext);
            } else {
                //FIXME: serialize correctly entityOrEvent
                sseManager.sendMessage(id,webNotif.getCode(),webNotif.getDescription(),entityOrEvent.toString(),oContext);
            }
        } catch (Exception e) {
            try {
                log.debug("WebNotification business error : ", e);
                notificationHistoryService.create(webNotif, entityOrEvent, e.getMessage(),NotificationHistoryStatusEnum.FAILED);
            } catch (BusinessException e2) {
                log.error("Failed to create notification history", e2);
            }
        }
    }
}
