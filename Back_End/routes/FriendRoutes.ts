import { FriendController } from "../controllers/FriendController";
import { body } from "express-validator";

const friendController = new FriendController();

export const FriendRoutes = [
    {
        method: "get",
        route: "/friends/:userID",
        controller: friendController,
        action: friendController.getFriends,
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/sendRequest/:friendEmail",
        controller: friendController,
        action: friendController.sendFriendRequest,
        validation: []
    },


    {
        method: "get",
        route: "/friends/:userID/friendRequests",
        controller: friendController,
        action: friendController.getFriendRequests,
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/acceptRequest/:friendID",
        controller: friendController,
        action: friendController.acceptFriendRequest,
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/declineRequest/:friendID",
        controller: friendController,
        action: friendController.declineFriendRequest,
        validation: []
    },

    {
        method: "put",
        route: "/friends/:userID/deleteFriend/:friendID",
        controller: friendController,
        action: friendController.deleteFriend,
        validation: []
    },
    
]