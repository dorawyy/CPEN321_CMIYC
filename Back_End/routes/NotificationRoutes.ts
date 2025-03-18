import { NotificationController } from "../controllers/NotificationController";
import { body} from "express-validator";
import { Request, Response } from "express";

const controller = new NotificationController();

export const NotificationRoutes = [
    {
        method: "put",
        route: "/fcm/:userID",
        action: (req: Request, res: Response) => controller.setFCMToken(req, res),
        validation: [body("fcmToken").isString()],
    },

    {
        method: "post",
        route: "/send-event/:userID",
        action: (req: Request, res: Response) => controller.sendEventNotification(req, res),
        validation: [body("eventName").isString()],
    },

    {
        method: "get",
        route: "/notifications/:userID",
        action: (req: Request, res: Response) => controller.getNotifications(req, res),
        validation: [],
    }
]
