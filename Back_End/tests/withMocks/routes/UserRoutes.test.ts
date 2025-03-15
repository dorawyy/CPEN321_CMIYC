import { Express } from 'express';
import { ObjectId } from 'mongodb';
import { setupTestApp, createTestRequest, testUserData } from '../../testSetup';
import '../../setupFirebaseMock'; // Import Firebase mocking

// Set a long timeout for the entire test suite
jest.setTimeout(3000);

// Mock the client from services
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

// Import and extract the mocked client
import { client } from '../../../services';
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
 * Tests for UserRoutes with mocking external dependencies
 * These tests use mocked database interactions
 */
describe('UserRoutes API - With Mocks', () => {

  /**
   * POST /user - Create User Profile
   * Tests the route for creating a new user profile
   */
  describe('POST /user - Create User Profile', () => {
    
    /**
     * Test: Successfully create a new user profile
     * Input: Valid user data
     * Expected Status: 200
     * Expected Output: Confirmation message with user ID
     * Mock Behavior: findOne returns null (user doesn't exist), insertOne succeeds
     */
    test('should create a new user profile when user doesnt exist', async () => {
      // Mock findOne to return null (user doesn't exist)
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      // Mock insertOne to return success
      const mockInsertedId = new ObjectId();
      (mockCollection.insertOne as jest.Mock).mockResolvedValueOnce({
        acknowledged: true,
        insertedId: mockInsertedId
      });
      
      const response = await createTestRequest(app)
        .post('/user')
        .send(testUserData);
      
      expect(response.status).toBe(200);
      expect(response.body.message).toContain('User profile created');
      expect(response.body.isAdmin).toBe(false);
      expect(response.body.isBanned).toBe(false);
      
      // Verify mocks were called correctly
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: testUserData.userID });
      expect(mockCollection.insertOne).toHaveBeenCalledWith(expect.objectContaining({
        userID: testUserData.userID,
        displayName: testUserData.displayName,
        friends: [],
        friendRequests: [],
        logList: [],
        isBanned: false
      }));
    });
    
    /**
     * Test: Return existing user profile
     * Input: User data with ID that already exists
     * Expected Status: 200
     * Expected Output: Message that user already exists
     * Mock Behavior: findOne returns existing user data
     */
    test('should return existing user when user already exists', async () => {
      // Mock findOne to return an existing user
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce({
        ...testUserData,
        _id: new ObjectId(),
        friends: [],
        friendRequests: [],
        logList: [],
        isBanned: false
      });
      
      const response = await createTestRequest(app)
        .post('/user')
        .send(testUserData);
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe('User profile already exists');
      expect(response.body.isAdmin).toBe(false);
      expect(response.body.isBanned).toBe(false);
      
      // Verify insertOne was not called
      expect(mockCollection.insertOne).not.toHaveBeenCalled();
    });
    
    /**
     * Test: Return banned user profile
     * Input: User data with ID that is banned
     * Expected Status: 200
     * Expected Output: Message that user is banned
     * Mock Behavior: findOne returns user with isBanned=true
     */
    test('should return banned status for banned user', async () => {
      // Mock findOne to return a banned user
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce({
        ...testUserData,
        _id: new ObjectId(),
        friends: [],
        friendRequests: [],
        logList: [],
        isBanned: true
      });
      
      const response = await createTestRequest(app)
        .post('/user')
        .send(testUserData);
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe('User is banned');
      expect(response.body.isAdmin).toBe(false);
      expect(response.body.isBanned).toBe(true);
      
      // Verify insertOne was not called
      expect(mockCollection.insertOne).not.toHaveBeenCalled();
    });
  });

  /**
   * GET /user/:userID - Get All Users
   * Tests the route for retrieving all users
   */
  describe('GET /user/:userID - Get All Users', () => {
    
    /**
     * Test: Successfully get all users
     * Input: Valid userID in URL parameter
     * Expected Status: 200
     * Expected Output: Array of users excluding the requesting user
     * Mock Behavior: find returns a list of users
     */
    test('should return all users except the requesting user', async () => {
      // Create mock users
      const mockUsers = [
        {
          _id: new ObjectId(),
          userID: 'other-user-1',
          displayName: 'Other User 1',
          email: 'other1@example.com',
          friends: ['friend1', 'friend2'],
          friendRequests: ['request1'],
          logList: [],
          isBanned: false
        },
        {
          _id: new ObjectId(),
          userID: 'other-user-2',
          displayName: 'Other User 2',
          email: 'other2@example.com',
          friends: ['friend3'],
          friendRequests: [],
          logList: [],
          isBanned: true
        }
      ];
      
      // Mock find to return the mock users
      const mockToArray = jest.fn().mockResolvedValueOnce(mockUsers);
      (mockCollection.find as jest.Mock).mockReturnValueOnce({
        toArray: mockToArray
      });
      
      const response = await createTestRequest(app)
        .get(`/user/${testUserData.userID}`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body.length).toBe(2);
      
      // Check that users don't have friends and friendRequests arrays
      expect(response.body[0].friends).toBeUndefined();
      expect(response.body[0].friendRequests).toBeUndefined();
      expect(response.body[1].friends).toBeUndefined();
      expect(response.body[1].friendRequests).toBeUndefined();
      
      // Verify find was called
      expect(mockCollection.find).toHaveBeenCalledWith({});
    });
  });

  /**
   * POST /user/ban/:userID - Ban User
   * Tests the route for banning a user
   */
  describe('POST /user/ban/:userID - Ban User', () => {
    
    /**
     * Test: Successfully ban a user
     * Input: Valid userID in URL parameter
     * Expected Status: 200
     * Expected Output: Confirmation message
     * Mock Behavior: findOne returns a user, updateOne succeeds
     */
    test('should ban a user', async () => {
      // Mock findOne to return a user
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce({
        ...testUserData,
        _id: new ObjectId(),
        isBanned: false
      });
      
      // Mock updateOne to return success
      (mockCollection.updateOne as jest.Mock).mockResolvedValueOnce({
        acknowledged: true,
        modifiedCount: 1
      });
      
      const response = await createTestRequest(app)
        .post(`/user/ban/${testUserData.userID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('User banned');
      
      // Verify mocks were called correctly
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: testUserData.userID });
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: testUserData.userID },
        { $set: { isBanned: true } }
      );
    });
    
    /**
     * Test: Try to ban a non-existent user
     * Input: Invalid userID in URL parameter
     * Expected Status: 404
     * Expected Output: User not found message
     * Mock Behavior: findOne returns null
     */
    test('should return 404 for non-existent user', async () => {
      // Mock findOne to return null (user doesn't exist)
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .post(`/user/ban/non-existent-user`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
  });
}); 