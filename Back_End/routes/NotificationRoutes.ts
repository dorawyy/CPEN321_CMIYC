import { NotificationController } from "../controllers/NotificationController";
import { body, param } from "express-validator";

const notificationController = new NotificationController();

export const NotificationRoutes = [
    {
        method: "get",
        route: "/fcm",
        controller: notificationController,
        action: notificationController.getFCMToken,
        validation: [body("name").isString()],
    },

    {
        method: "put",
        route: "/fcm",
        controller: notificationController,
        action: notificationController.setFCMToken,
        validation: [body("fcmToken").isString(), body("name").isString()],
    },

    {
        method: "post",
        route: "/send-event",
        controller: notificationController,
        action: notificationController.sendEventNotification,
        validation: [body("name").isString()],
    },

    {
        method: "get",
        route: "/notifications",
        controller: notificationController,
        action: notificationController.getNotifications,
        validation: [body("name").isString()],
    }
]
