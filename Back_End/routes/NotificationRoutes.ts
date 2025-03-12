import { NotificationController } from "../controllers/NotificationController";
import { body} from "express-validator";

const controller = new NotificationController();

export const NotificationRoutes = [
    {
        method: "put",
        route: "/fcm/:userID",
        action: controller.setFCMToken,
        validation: [body("fcmToken").isString()],
    },

    {
        method: "post",
        route: "/send-event/:userID",
        action: controller.sendEventNotification,
        validation: [body("eventName").isString()],
    },

    {
        method: "get",
        route: "/notifications/:userID",
        action: controller.getNotifications,
        validation: [],
    }
]
