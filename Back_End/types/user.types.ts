export interface User {
    userID: string;
    displayName: string;
    email: string;
    photoURL: string;
    fcmToken: string;
    currentLocation: {
      latitude: number;
      longitude: number;
      timestamp: number;
    };
    friends: string[]
    friendRequests: string[]
  }