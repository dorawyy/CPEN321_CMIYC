import { Express } from 'express';
import { ObjectId } from 'mongodb';
import { setupTestApp, createTestRequest, testUserData } from '../../testSetup';
import '../../setupFirebaseMock'; // Import Firebase mocking
import { client, messaging } from '../../../services';

// Set a long timeout for the entire test suite
jest.setTimeout(3000);

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

// Setup the test app
let app: Express;

beforeAll(() => {
  app = setupTestApp();
});

beforeEach(() => {
  // Reset all mocks before each test
  jest.clearAllMocks();
});

/**
 * Tests for NotificationRoutes with mocking external dependencies
 * These tests use mocked database interactions
 */
describe('NotificationRoutes API - With Mocks', () => {

  /**
   * PUT /fcm/:userID - Set FCM Token
   * Tests the route for setting a user's FCM token
   */
  describe('PUT /fcm/:userID - Set FCM Token', () => {
    
    /**
     * Test: Successfully update FCM token
     * Input: Valid userID and FCM token
     * Expected Status: 200
     * Expected Output: Confirmation message
     * Mock Behavior: findOne returns user, updateOne succeeds
     */
    test('should update FCM token for existing user', async () => {
      // Mock findOne to return a user
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce({
        ...testUserData,
        _id: new ObjectId(),
        fcmToken: 'old-token'
      });
      
      // Mock updateOne to return success
      (mockCollection.updateOne as jest.Mock).mockResolvedValueOnce({
        matchedCount: 1,
        modifiedCount: 1,
        acknowledged: true
      });
      
      const response = await createTestRequest(app)
        .put(`/fcm/${testUserData.userID}`)
        .send({ fcmToken: 'new-token' });
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe('FCM token set successfully');
      
      // Verify mocks were called correctly
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: testUserData.userID });
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: testUserData.userID },
        { $set: { fcmToken: 'new-token' } }
      );
    });
    
    /**
     * Test: Try to update FCM token for non-existent user
     * Input: Invalid userID and valid FCM token
     * Expected Status: 404
     * Expected Output: User not found message
     * Mock Behavior: findOne returns null
     */
    test('should return 404 for non-existent user', async () => {
      // Mock findOne to return null (user doesn't exist)
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .put(`/fcm/non-existent-user`)
        .send({ fcmToken: 'some-token' });
      
      expect(response.status).toBe(404);
      expect(response.body.message).toBe('User not found');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
    
    /**
     * Test: Failed update FCM token
     * Input: Valid userID and FCM token
     * Expected Status: 404
     * Expected Output: Error message
     * Mock Behavior: findOne returns user, updateOne fails
     */
    test('should return 404 if update fails', async () => {
      // Mock findOne to return a user
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce({
        ...testUserData,
        _id: new ObjectId(),
        fcmToken: 'old-token'
      });
      
      // Mock updateOne to return failure
      (mockCollection.updateOne as jest.Mock).mockResolvedValueOnce({
        matchedCount: 0,
        modifiedCount: 0,
        acknowledged: true
      });
      
      const response = await createTestRequest(app)
        .put(`/fcm/${testUserData.userID}`)
        .send({ fcmToken: 'new-token' });
      
      expect(response.status).toBe(404);
      expect(response.body.message).toBe('Failed to update FCM token');
    });
    
    /**
     * Test: Validation error for missing FCM token
     * Input: Valid userID but missing FCM token
     * Expected Status: 400
     * Expected Output: Validation errors
     * Mock Behavior: Controller not called
     */
    test('should return validation errors for missing FCM token', async () => {
      const response = await createTestRequest(app)
        .put(`/fcm/${testUserData.userID}`)
        .send({}); // Missing fcmToken
      
      expect(response.status).toBe(400);
      expect(response.body.errors).toBeTruthy();
      
      // Verify controller methods weren't called
      expect(mockCollection.findOne).not.toHaveBeenCalled();
    });
  });

  /**
   * GET /notifications/:userID - Get Notifications
   * Tests the route for retrieving a user's notifications
   */
  describe('GET /notifications/:userID - Get Notifications', () => {
    
    /**
     * Test: Successfully get notifications for user
     * Input: Valid userID
     * Expected Status: 200
     * Expected Output: Array of notifications (logList)
     * Mock Behavior: findOne returns user with logList
     */
    test('should return notifications for existing user', async () => {
      // Create mock logList
      const mockLogList = [
        {
          fromName: 'Friend 1',
          eventName: 'Event 1',
          location: { latitude: 49.2827, longitude: -123.1207 }
        },
        {
          fromName: 'Friend 2',
          eventName: 'Event 2',
          location: { latitude: 49.2828, longitude: -123.1208 }
        }
      ];
      
      // Mock findOne to return a user with logList
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce({
        ...testUserData,
        _id: new ObjectId(),
        logList: mockLogList
      });
      
      const response = await createTestRequest(app)
        .get(`/notifications/${testUserData.userID}`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body).toEqual(mockLogList);
      
      // Verify mock was called correctly
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: testUserData.userID });
    });
    
    /**
     * Test: Try to get notifications for non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     * Mock Behavior: findOne returns null
     */
    test('should return 404 for non-existent user', async () => {
      // Mock findOne to return null (user doesn't exist)
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .get(`/notifications/non-existent-user`);
      
      expect(response.status).toBe(404);
      expect(response.body.message).toBe('User not found');
    });

    /**
     * Test: Get notifications for user without logList property
     * Input: Valid userID for user without logList property
     * Expected Status: 200
     * Expected Output: undefined (which will be converted to null in JSON)
     * Mock Behavior: findOne returns user without logList property
     */
    test('should handle user without logList property', async () => {
      // Mock findOne to return a user without logList
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce({
        ...testUserData,
        _id: new ObjectId(),
        // No logList property
      });
      
      const response = await createTestRequest(app)
        .get(`/notifications/${testUserData.userID}`);
      
      expect(response.status).toBe(200);
      // When user.logList is undefined, res.send(user.logList) in lines 153-154
      // of NotificationController will return undefined, which Express converts to {}
      expect(response.body).toEqual({});
      
      // Verify mock was called correctly
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: testUserData.userID });
    });

    /**
     * Test: Get notifications for user with empty logList
     * Input: Valid userID for user with empty logList array
     * Expected Status: 200
     * Expected Output: Empty array
     * Mock Behavior: findOne returns user with empty logList array
     */
    test('should return empty array for user with empty logList', async () => {
      // Mock findOne to return a user with empty logList
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce({
        ...testUserData,
        _id: new ObjectId(),
        logList: [] // Empty array (not undefined)
      });
      
      const response = await createTestRequest(app)
        .get(`/notifications/${testUserData.userID}`);
      
      expect(response.status).toBe(200);
      expect(response.body).toEqual([]);
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: testUserData.userID });
    });
  });

  /**
   * POST /send-event/:userID - Send Event Notification
   * Tests the route for sending event notifications to nearby friends
   */
  describe('POST /send-event/:userID - Send Event Notification', () => {
    
    /**
     * Test: Successfully send notification to nearby friends
     * Input: Valid userID and event name
     * Expected Status: 200
     * Expected Output: Success message
     * Mock Behavior: findOne returns user with location, find returns friends, updateOne succeeds
     */
    test('should send notification to nearby friends', async () => {
      // Mock user with location
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        displayName: 'Test User',
        friends: ['friend1', 'friend2'],
        currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() }
      };
      
      // Mock findOne to return the user
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      
      // Mock nearby friends
      const mockFriends = [
        {
          _id: new ObjectId(),
          userID: 'friend1',
          displayName: 'Friend 1',
          fcmToken: 'token1',
          currentLocation: { latitude: 49.2827, longitude: -123.1208, timestamp: Date.now() }
        },
        {
          _id: new ObjectId(),
          userID: 'friend2',
          displayName: 'Friend 2',
          fcmToken: 'token2',
          currentLocation: { latitude: 49.2828, longitude: -123.1209, timestamp: Date.now() }
        }
      ];
      
      // Mock find to return friends
      (mockCollection.find as jest.Mock).mockReturnValueOnce({
        toArray: jest.fn().mockResolvedValueOnce(mockFriends)
      });
      
      // Mock updateOne for checking logList existence
      (mockCollection.updateOne as jest.Mock).mockResolvedValue({
        matchedCount: 1,
        modifiedCount: 1,
        acknowledged: true
      });
      
      // Mock messaging.send to succeed
      (messaging.send as jest.Mock).mockResolvedValue({ success: true });
      
      const response = await createTestRequest(app)
        .post(`/send-event/${testUserData.userID}`)
        .send({ eventName: 'Test Event' });
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe('Notification sent successfully');
      
      // Verify findOne was called correctly
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: testUserData.userID });
      
      // Verify find was called correctly
      expect(mockCollection.find).toHaveBeenCalledWith({ 
        userID: { $in: mockUser.friends }, 
        isBanned: { $ne: true } 
      });
      
      // Verify updateOne was called for each friend (at least once)
      expect(mockCollection.updateOne).toHaveBeenCalled();
      
      // Verify messaging.send was called twice (once for each friend)
      expect(messaging.send).toHaveBeenCalledTimes(2);
    });
    
    /**
     * Test: User not found
     * Input: Invalid userID, valid event name
     * Expected Status: 404
     * Expected Output: User not found message
     * Mock Behavior: findOne returns null
     */
    test('should return 404 for non-existent user', async () => {
      // Mock findOne to return null (user doesn't exist)
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .post(`/send-event/non-existent-user`)
        .send({ eventName: 'Test Event' });
      
      expect(response.status).toBe(404);
      expect(response.body.message).toBe('User not found');
      
      // Verify find and updateOne were not called
      expect(mockCollection.find).not.toHaveBeenCalled();
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
      expect(messaging.send).not.toHaveBeenCalled();
    });
    
    /**
     * Test: User without location
     * Input: Valid userID but user has no location
     * Expected Status: 400
     * Expected Output: User location not set message
     * Mock Behavior: findOne returns user without location
     */
    test('should return 400 if user location not set', async () => {
      // Mock user without location
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: ['friend1', 'friend2'],
        currentLocation: null // No location
      };
      
      // Mock findOne to return the user without location
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      
      const response = await createTestRequest(app)
        .post(`/send-event/${testUserData.userID}`)
        .send({ eventName: 'Test Event' });
      
      expect(response.status).toBe(400);
      expect(response.body.message).toBe('User location not set');
      
      // Verify find and updateOne were not called
      expect(mockCollection.find).not.toHaveBeenCalled();
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
      expect(messaging.send).not.toHaveBeenCalled();
    });
    
    /**
     * Test: Validation error for missing event name
     * Input: Valid userID but missing event name
     * Expected Status: 400
     * Expected Output: Validation errors
     * Mock Behavior: Controller not called
     */
    test('should return validation errors for missing event name', async () => {
      const response = await createTestRequest(app)
        .post(`/send-event/${testUserData.userID}`)
        .send({}); // Missing eventName
      
      expect(response.status).toBe(400);
      expect(response.body.errors).toBeTruthy();
      
      // Verify controller methods weren't called
      expect(mockCollection.findOne).not.toHaveBeenCalled();
      expect(mockCollection.find).not.toHaveBeenCalled();
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
      expect(messaging.send).not.toHaveBeenCalled();
    });

    /**
     * Test: Handle Firebase messaging error
     * Input: Valid userID and event name
     * Expected Status: 500
     * Expected Output: Error message
     * Mock Behavior: messaging.send throws an error to trigger catch block
     */
    test('should handle Firebase messaging error', async () => {
      // Mock user with location
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        displayName: 'Test User',
        friends: ['friend1'],
        currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() }
      };
      
      // Mock findOne to return the user
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      
      // Mock nearby friend with invalid FCM token
      const mockFriends = [
        {
          _id: new ObjectId(),
          userID: 'friend1',
          displayName: 'Friend 1',
          fcmToken: 'invalid-token', // This token will cause an error
          currentLocation: { latitude: 49.2827, longitude: -123.1208, timestamp: Date.now() }
        }
      ];
      
      // Mock find to return friends
      (mockCollection.find as jest.Mock).mockReturnValueOnce({
        toArray: jest.fn().mockResolvedValueOnce(mockFriends)
      });
      
      // Mock updateOne for checking logList existence
      (mockCollection.updateOne as jest.Mock).mockResolvedValue({
        matchedCount: 1,
        modifiedCount: 1,
        acknowledged: true
      });
      
      // Mock messaging.send to throw an error
      (messaging.send as jest.Mock).mockRejectedValueOnce(new Error('Invalid registration token'));
      
      const response = await createTestRequest(app)
        .post(`/send-event/${testUserData.userID}`)
        .send({ eventName: 'Test Event' });
      
      // This should hit the catch block in sendEventNotification
      expect(response.status).toBe(500);
      expect(response.text).toBe('Error sending notification');
      
      // Verify messaging.send was called with the invalid token
      expect(messaging.send).toHaveBeenCalledWith({
        token: 'invalid-token',
        notification: {
          title: 'Test Event',
          body: `${mockUser.displayName} is starting a new event!`
        }
      });
    });
  });
}); 