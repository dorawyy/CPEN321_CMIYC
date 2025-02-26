import { NotificationController } from "../controllers/NotificationController";
import { body, param } from "express-validator";

const notificationController = new NotificationController();

export const NotificationRoutes = [
    {
        method: "put",
        route: "/fcm/:userID",
        controller: notificationController,
        action: notificationController.setFCMToken,
        validation: [body("fcmToken").isString()],
    },

    {
        method: "post",
        route: "/send-event/:userID",
        controller: notificationController,
        action: notificationController.sendEventNotification,
        validation: [body("eventName").isString()],
    },

    {
        method: "get",
        route: "/notifications/:userID",
        controller: notificationController,
        action: notificationController.getNotifications,
        validation: [],
    }
]
