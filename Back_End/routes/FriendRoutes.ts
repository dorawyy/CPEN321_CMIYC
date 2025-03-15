import { FriendController } from "../controllers/FriendController";

const controller = new FriendController();

export const FriendRoutes = [
    {
        method: "get",
        route: "/friends/:userID",
        action: controller.getFriends,
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/sendRequest/:friendEmail",
        action: controller.sendFriendRequest,
        validation: []
    },


    {
        method: "get",
        route: "/friends/:userID/friendRequests",
        action: controller.getFriendRequests,
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/acceptRequest/:friendID",
        action: controller.acceptFriendRequest,
        validation: []
    },

    {
        method: "post",
        route: "/friends/:userID/declineRequest/:friendID",
        action: controller.declineFriendRequest,
        validation: []
    },

    {
        method: "put",
        route: "/friends/:userID/deleteFriend/:friendID",
        action: controller.deleteFriend,
        validation: []
    },
    
]