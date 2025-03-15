import { Express } from 'express';
import { ObjectId } from 'mongodb';
import { setupTestApp, createTestRequest, testUserData } from '../../testSetup';
import { client } from '../../../services';
import '../../setupFirebaseMock'; // Import Firebase mocking

// Make sure environment variables are loaded before Firebase is initialized
import dotenv from 'dotenv';
dotenv.config();

// Set a long timeout for the entire test suite
jest.setTimeout(3000);

// Setup the test app
let app: Express;

beforeAll(async () => {
  app = setupTestApp();
  // Connect to the actual database for non-mocked tests
  try {
    await client.connect();
  } catch (error) {
    console.error('Error connecting to database:', error);
    throw error;
  }
}, 3000); // 3 seconds timeout for beforeAll

afterAll(async () => {
  // Clean up test data
  try {
    await client.db("cmiyc").collection("users").deleteOne({ userID: testUserData.userID });
    await client.close();
  } catch (error) {
    console.error('Error during cleanup:', error);
  }
}, 3000); // 3 seconds timeout for afterAll

/**
 * Tests for UserRoutes without mocking external dependencies
 * These tests use the actual database connection
 */
describe('UserRoutes API - No Mocks', () => {

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
     * Expected Behavior: User should be saved in the database
     */
    test('should create a new user profile with valid data', async () => {
      const response = await createTestRequest(app)
        .post('/user')
        .send(testUserData);
      
      expect(response.status).toBe(200);
      expect(response.body.message).toContain('User profile created');
      expect(response.body.isAdmin).toBe(false);
      expect(response.body.isBanned).toBe(false);
      
      // Verify user was created in the database
      const createdUser = await client.db("cmiyc").collection("users").findOne({ userID: testUserData.userID });
      expect(createdUser).toBeTruthy();
      expect(createdUser?.displayName).toBe(testUserData.displayName);
    });
    
    /**
     * Test: Return existing user profile
     * Input: User data with ID that already exists
     * Expected Status: 200
     * Expected Output: Message that user already exists
     * Expected Behavior: No new user should be created
     */
    test('should return existing user profile when userID already exists', async () => {
      // Try to create the same user again
      const response = await createTestRequest(app)
        .post('/user')
        .send(testUserData);
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe('User profile already exists');
      expect(response.body.isAdmin).toBe(false);
      expect(response.body.isBanned).toBe(false);
    });
    
    /**
     * Test: Validation error for missing fields
     * Input: User data with missing required fields
     * Expected Status: 400
     * Expected Output: Validation errors
     * Expected Behavior: No user should be created
     */
    test('should return validation errors for missing fields', async () => {
      const incompleteData = {
        userID: "incomplete-user",
        // Missing other required fields
      };
      
      const response = await createTestRequest(app)
        .post('/user')
        .send(incompleteData);
      
      expect(response.status).toBe(400);
      expect(response.body.errors).toBeTruthy();
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
     * Expected Behavior: User list should not include friends and friendRequests arrays
     */
    test('should return all users except the requesting user', async () => {
      const response = await createTestRequest(app)
        .get(`/user/${testUserData.userID}`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      
      // Make sure the requesting user is not included in the results
      const foundUsers = response.body.filter((user: any) => user.userID === testUserData.userID);
      expect(foundUsers.length).toBe(0);
      
      // Check that users don't have friends and friendRequests arrays
      if (response.body.length > 0) {
        expect(response.body[0].friends).toBeUndefined();
        expect(response.body[0].friendRequests).toBeUndefined();
      }
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
     * Expected Behavior: User's isBanned field should be set to true
     */
    test('should ban a user', async () => {
      const response = await createTestRequest(app)
        .post(`/user/ban/${testUserData.userID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('User banned');
      
      // Verify user is banned in the database
      const bannedUser = await client.db("cmiyc").collection("users").findOne({ userID: testUserData.userID });
      expect(bannedUser?.isBanned).toBe(true);
    });
    
    /**
     * Test: Try to ban a non-existent user
     * Input: Invalid userID in URL parameter
     * Expected Status: 404
     * Expected Output: User not found message
     * Expected Behavior: No changes to database
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .post(`/user/ban/${nonExistentUserID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
    });
  });
}); 