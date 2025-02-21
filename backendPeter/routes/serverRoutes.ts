import { ServerController } from "../controllers/serverController";
import { body, param } from "express-validator";

const controller = new ServerController();
export const serverRoutes = [
    {
        method: "get",
        route: "/fcm",
        controller: ServerController,
        action: controller.getFCMToken,
        validation: [body("name").isString()],
    },

    {
        method: "put",
        route: "/fcm",
        controller: ServerController,
        action: controller.setFCMToken,
        validation: [body("fcmToken").isString(), body("name").isString()],
    },

    {
        method: "put",
        route: "/location",
        controller: ServerController,
        action: controller.updateLocation,
        validation: [body("name").isString(), body("location").isObject()],
    },

    {
        method: "post",
        route: "/send-event",
        controller: ServerController,
        action: controller.sendEventNotification,
        validation: [body("name").isString()],
    },
]
