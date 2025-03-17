import { Express } from 'express';
import { setupTestApp, createTestRequest, testUserData } from '../../testSetup';
import { client } from '../../../services';
import '../../setupFirebaseMock';

// Make sure environment variables are loaded before Firebase is initialized
import dotenv from 'dotenv';
dotenv.config();

// Set a longer timeout for performance tests
jest.setTimeout(3000);

// Setup the test app
let app: Express;

beforeAll(async () => {
  app = setupTestApp();
  
  // Connect to the actual database for non-mocked tests
  try {
    await client.connect();
    
    // Create a test user for performance tests if needed
    const existingUser = await client.db("cmiyc").collection("users").findOne({ userID: testUserData.userID });
    if (!existingUser) {
      await client.db("cmiyc").collection("users").insertOne({
        ...testUserData,
        friends: [],
        friendRequests: [],
        logList: []
      });
    }
  } catch (error) {
    console.error('Error connecting to database:', error);
    throw error;
  }
});

afterAll(async () => {
  // Clean up test data
  try {
    await client.db("cmiyc").collection("users").deleteOne({ userID: testUserData.userID });
    await client.close();
  } catch (error) {
    console.error('Error during cleanup:', error);
  }
});

/**
 * Non-functional requirement tests for performance
 * Each endpoint should respond in under 0.5 seconds on average
 */
describe('Performance Tests - Non-functional Requirements', () => {
  
  // Test updateUserLocation performance (10 calls)
  test('updateUserLocation should respond in under 0.5 seconds on average', async () => {
    const testRequest = createTestRequest(app);
    const iterations = 10;
    const responseTimes = [];
    
    for (let i = 0; i < iterations; i++) {
      const updateData = {
        currentLocation: {
          latitude: 49.2827 + (Math.random() * 0.01),
          longitude: -123.1207 + (Math.random() * 0.01),
          timestamp: Date.now()
        }
      };
      
      const startTime = Date.now();
      
      await testRequest
        .put(`/location/${testUserData.userID}`)
        .send(updateData)
        .expect(200);
        
      const endTime = Date.now();
      const responseTime = endTime - startTime;
      responseTimes.push(responseTime);
    }
    
    const averageResponseTime = responseTimes.reduce((sum, time) => sum + time, 0) / iterations;
    // console.log(`Average response time for updateUserLocation: ${averageResponseTime} ms`);
    
    expect(averageResponseTime).toBeLessThan(500); // 0.5 seconds = 500 ms
  });
  
  // Test getFriends performance (10 calls)
  test('getFriends should respond in under 0.5 seconds on average', async () => {
    const testRequest = createTestRequest(app);
    const iterations = 10;
    const responseTimes = [];
    
    for (let i = 0; i < iterations; i++) {
      const startTime = Date.now();
      
      await testRequest
        .get(`/friends/${testUserData.userID}`)
        .expect(200);
        
      const endTime = Date.now();
      const responseTime = endTime - startTime;
      responseTimes.push(responseTime);
    }
    
    const averageResponseTime = responseTimes.reduce((sum, time) => sum + time, 0) / iterations;
    // console.log(`Average response time for getFriends: ${averageResponseTime} ms`);
    
    expect(averageResponseTime).toBeLessThan(500); // 0.5 seconds = 500 ms
  });
  
  // Test getNotifications performance (10 calls)
  test('getNotifications should respond in under 0.5 seconds on average', async () => {
    const testRequest = createTestRequest(app);
    const iterations = 10;
    const responseTimes = [];
    
    for (let i = 0; i < iterations; i++) {
      const startTime = Date.now();
      
      await testRequest
        .get(`/notifications/${testUserData.userID}`)
        .expect(200);
        
      const endTime = Date.now();
      const responseTime = endTime - startTime;
      responseTimes.push(responseTime);
    }
    
    const averageResponseTime = responseTimes.reduce((sum, time) => sum + time, 0) / iterations;
    // console.log(`Average response time for getNotifications: ${averageResponseTime} ms`);
    
    expect(averageResponseTime).toBeLessThan(500); // 0.5 seconds = 500 ms
  });
}); 