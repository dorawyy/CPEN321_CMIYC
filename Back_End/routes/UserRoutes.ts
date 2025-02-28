import { UserController } from "../controllers/UserController";
import { body } from "express-validator";

const controller = new UserController();

export const UserRoutes = [
    {
        method: "post",
        route: "/user",
        action: controller.createUserProfile,
        validation: [
            body("userID").notEmpty().isString(),
            body("name").notEmpty().isString(),
            body("email").notEmpty().isString().isEmail(),
        ]
    },
    {
        method: "get",
        route: "/user/:userID",
        action: controller.getUserProfile,
        validation: []
    },
    {
        method: "put",
        route: "/user/:userID",
        action: controller.updateUserProfile,
        validation: [
            body("userID").notEmpty().isString()
        ]
    },
    {
        method: "delete",
        route: "/user/:userID",
        action: controller.deleteUserProfile,
        validation: [
            body("userID").notEmpty().isString()
        ]
    },
]