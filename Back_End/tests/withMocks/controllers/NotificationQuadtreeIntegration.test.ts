import { NotificationController } from '../../../controllers/NotificationController';
import { Request, Response, NextFunction } from 'express';
import { client, messaging } from '../../../services';

// Mock the client and messaging from services
jest.mock('../../../services', () => {
  const mockCollection = {
    findOne: jest.fn(),
    insertOne: jest.fn(),
    updateOne: jest.fn(),
    find: jest.fn(),
  };
  
  const mockDb = {
    collection: jest.fn(() => mockCollection)
  };
  
  return {
    client: {
      connect: jest.fn(),
      close: jest.fn(),
      db: jest.fn(() => mockDb)
    },
    messaging: {
      send: jest.fn().mockResolvedValue({ success: true }),
    }
  };
});

const mockDb = client.db('cmiyc');
const mockCollection = mockDb.collection('users');

// Mock the Quadtree class to isolate testing of sendEventNotification that uses the quadtree
jest.mock('../../../utils/Quadtree');

describe('NotificationController - Integration with Quadtree', () => {
  let notificationController: NotificationController;
  let mockRequest: Partial<Request>;
  let mockResponse: Partial<Response>;
  let mockNext: NextFunction = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    
    notificationController = new NotificationController();
    
    mockRequest = {
      params: { userID: 'user123' },
      body: { eventName: 'Test Event' }
    };
    
    mockResponse = {
      status: jest.fn().mockReturnThis(),
      send: jest.fn()
    };
    
    mockNext = jest.fn();
    
    // Mock the static findNearbyFriendsWithQuadtree method
    jest.spyOn(NotificationController, 'findNearbyFriendsWithQuadtree')
      .mockImplementation(() => []); // Default to empty array
  });

  test('should call findNearbyFriendsWithQuadtree with user and friends', async () => {
    // Mock user data
    const mockUser = {
      userID: 'user123',
      displayName: 'Test User',
      currentLocation: {
        latitude: 49.2827,
        longitude: -123.1207
      }
    };
    
    // Mock friends data
    const mockFriends = [
      {
        userID: 'friend1',
        displayName: 'Friend 1',
        currentLocation: {
          latitude: 49.2828,
          longitude: -123.1208
        },
        fcmToken: 'token1'
      }
    ];
    
    // Set up mocks
    (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
    (mockCollection.find as jest.Mock).mockReturnValueOnce({
      toArray: jest.fn().mockResolvedValueOnce(mockFriends)
    });
    
    // Mock findNearbyFriendsWithQuadtree to return our friend
    const findNearbyFriendsSpy = jest.spyOn(NotificationController, 'findNearbyFriendsWithQuadtree')
      .mockImplementation(() => mockFriends);
    
    // Call the method
    await notificationController.sendEventNotification(
      mockRequest as Request,
      mockResponse as Response
    );
    
    // Verify findNearbyFriendsWithQuadtree was called with correct params
    expect(findNearbyFriendsSpy).toHaveBeenCalledWith(mockUser, mockFriends);
    
    // Verify the response
    expect(mockResponse.status).toHaveBeenCalledWith(200);
    expect(mockResponse.send).toHaveBeenCalledWith({
      message: "Notification sent successfully"
    });
  });

  test('should handle error when user not found', async () => {
    // Mock findOne to return null (user not found)
    (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
    
    // Call the method
    await notificationController.sendEventNotification(
      mockRequest as Request,
      mockResponse as Response
    );
    
    // Verify the response
    expect(mockResponse.status).toHaveBeenCalledWith(404);
    expect(mockResponse.send).toHaveBeenCalledWith({ 
      message: "User not found" 
    });
    
    // Verify findNearbyFriendsWithQuadtree was NOT called
    expect(NotificationController['findNearbyFriendsWithQuadtree']).not.toHaveBeenCalled();
  });

  test('should handle error when user location not set', async () => {
    // Mock user data without location
    const mockUser = {
      userID: 'user123',
      displayName: 'Test User',
      // No currentLocation
    };
    
    // Set up mocks
    (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
    
    // Call the method
    await notificationController.sendEventNotification(
      mockRequest as Request,
      mockResponse as Response
    );
    
    // Verify the response
    expect(mockResponse.status).toHaveBeenCalledWith(400);
    expect(mockResponse.send).toHaveBeenCalledWith({ 
      message: "User location not set" 
    });
    
    // Verify findNearbyFriendsWithQuadtree was NOT called
    expect(NotificationController['findNearbyFriendsWithQuadtree']).not.toHaveBeenCalled();
  });

  test('should update logList and send FCM notifications for nearby friends', async () => {
    // Mock user data
    const mockUser = {
      userID: 'user123',
      displayName: 'Test User',
      currentLocation: {
        latitude: 49.2827,
        longitude: -123.1207
      }
    };
    
    // Mock friends data
    const mockFriends = [
      {
        userID: 'friend1',
        displayName: 'Friend 1',
        currentLocation: {
          latitude: 49.2828,
          longitude: -123.1208
        },
        fcmToken: 'token1'
      },
      {
        userID: 'friend2',
        displayName: 'Friend 2',
        currentLocation: {
          latitude: 49.2830,
          longitude: -123.1210
        },
        fcmToken: 'token2'
      }
    ];
    
    // Set up mocks
    (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
    (mockCollection.find as jest.Mock).mockReturnValueOnce({
      toArray: jest.fn().mockResolvedValueOnce(mockFriends)
    });
    
    // Mock findNearbyFriendsWithQuadtree to return our friends
    jest.spyOn(NotificationController, 'findNearbyFriendsWithQuadtree')
      .mockImplementation(() => mockFriends);
    
    // Call the method
    await notificationController.sendEventNotification(
      mockRequest as Request,
      mockResponse as Response
    );
    
    // Verify updateOne was called for each friend to update logList
    expect(mockCollection.updateOne).toHaveBeenCalledTimes(4); // 2 for logList check + 2 for updates
    
    // Verify the first updateOne call for friend1 (ensuring logList exists)
    expect(mockCollection.updateOne).toHaveBeenNthCalledWith(
      1,
      { userID: 'friend1', logList: { $exists: false } },
      { $set: { logList: [] } }
    );
    
    // Verify the second updateOne call for friend1 (updating logList)
    expect(mockCollection.updateOne).toHaveBeenNthCalledWith(
      2,
      { userID: 'friend1' },
      { 
        $push: { 
          logList: {
            fromName: 'Test User',
            eventName: 'Test Event',
            location: mockUser.currentLocation,
          }
        } 
      }
    );
    
    // Verify FCM messages were sent to both friends
    expect(messaging.send).toHaveBeenCalledTimes(2);
    expect(messaging.send).toHaveBeenNthCalledWith(
      1,
      {
        token: 'token1',
        notification: {
          title: 'Test Event',
          body: 'Test User is starting a new event!'
        }
      }
    );
    expect(messaging.send).toHaveBeenNthCalledWith(
      2,
      {
        token: 'token2',
        notification: {
          title: 'Test Event',
          body: 'Test User is starting a new event!'
        }
      }
    );
    
    // Verify the response
    expect(mockResponse.status).toHaveBeenCalledWith(200);
    expect(mockResponse.send).toHaveBeenCalledWith({
      message: "Notification sent successfully"
    });
  });

  // Test specifically for line 136: handling friends without FCM tokens
  test('should handle friends without FCM tokens (line 136)', async () => {
    // Mock user data
    const mockUser = {
      userID: 'user123',
      displayName: 'Test User',
      currentLocation: {
        latitude: 49.2827,
        longitude: -123.1207
      }
    };
    
    // Mock friends data with a mix of friends with and without FCM tokens
    const mockFriends = [
      {
        userID: 'friend1',
        displayName: 'Friend 1',
        currentLocation: {
          latitude: 49.2828,
          longitude: -123.1208
        },
        fcmToken: 'token1'  // Has token
      },
      {
        userID: 'friend2',
        displayName: 'Friend 2',
        currentLocation: {
          latitude: 49.2830,
          longitude: -123.1210
        }
        // No fcmToken property - this should trigger line 136
      },
      {
        userID: 'friend3',
        displayName: 'Friend 3',
        currentLocation: {
          latitude: 49.2831,
          longitude: -123.1211
        },
        fcmToken: null  // null token - should also trigger line 136
      },
      {
        userID: 'friend4',
        displayName: 'Friend 4',
        currentLocation: {
          latitude: 49.2832,
          longitude: -123.1212
        },
        fcmToken: 'token4'  // Has token
      }
    ];
    
    // Set up database mocks
    (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
    (mockCollection.find as jest.Mock).mockReturnValueOnce({
      toArray: jest.fn().mockResolvedValueOnce([])  // Not important for this test
    });
    
    // Mock findNearbyFriendsWithQuadtree to return our mix of friends
    jest.spyOn(NotificationController, 'findNearbyFriendsWithQuadtree')
      .mockImplementation(() => mockFriends);
    
    // Call the method
    await notificationController.sendEventNotification(
      mockRequest as Request,
      mockResponse as Response
    );
    
    // Verify updateOne was called for all friends to update logList (8 calls = 4 friends × 2 updates each)
    expect(mockCollection.updateOne).toHaveBeenCalledTimes(8);
    
    // Verify FCM messages were sent only to friends with FCM tokens (2 out of 4 friends)
    expect(messaging.send).toHaveBeenCalledTimes(2);
    
    // Verify the tokens used were the ones we expect
    const sendCalls = (messaging.send as jest.Mock).mock.calls;
    const tokensUsed = sendCalls.map(call => call[0].token);
    expect(tokensUsed).toContain('token1');
    expect(tokensUsed).toContain('token4');
    expect(tokensUsed).not.toContain(null);
    expect(tokensUsed).not.toContain(undefined);
    
    // Verify the response
    expect(mockResponse.status).toHaveBeenCalledWith(200);
    expect(mockResponse.send).toHaveBeenCalledWith({
      message: "Notification sent successfully"
    });
  });
}); 