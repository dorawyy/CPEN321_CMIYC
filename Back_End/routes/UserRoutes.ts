import { UserController } from "../controllers/UserController";
import { body } from "express-validator";

const userController = new UserController();

export const UserRoutes = [
    {
        method: "post",
        route: "/user",
        controller: userController,
        action: userController.createUserProfile,
        validation: [
            body("userID").notEmpty().isString(),
            body("displayName").notEmpty().isString(),
            body("email").notEmpty().isString().isEmail(),
            body("photoURL").notEmpty().isString(),
            body("fcmToken").notEmpty().isString(),
            body("currentLocation").notEmpty().isObject(), // { latitude: number, longitude: number, timestamp: number }
        ]
    },

    {
        method: "get",
        route: "/user/:userID",
        controller: userController,
        action: userController.getAllUsers,
        validation: []
    },

    {
        method: "post",
        route: "/user/ban/:userID",
        controller: userController,
        action: userController.banUser,
        validation: [body("adminID").notEmpty().isString()]
    }
]