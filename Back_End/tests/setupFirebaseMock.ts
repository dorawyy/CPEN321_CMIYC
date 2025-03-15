import dotenv from 'dotenv';

// Make sure environment variables are loaded
dotenv.config();

// Mock Firebase admin for tests
jest.mock('firebase-admin', () => {
  const messagingMock = {
    send: jest.fn().mockResolvedValue({ success: true }),
  };
  
  return {
    initializeApp: jest.fn().mockImplementation(() => {
      console.log('Firebase mock initialized');
      return {};
    }),
    credential: {
      cert: jest.fn().mockImplementation((config) => {
        // Ensure we have the required fields
        if (!config?.projectId) {
          console.log('Using mock project ID');
          config = {
            projectId: 'mock-project-id',
            clientEmail: 'mock@example.com',
            privateKey: 'mock-key',
          };
        }
        return config;
      }),
    },
    messaging: jest.fn().mockReturnValue(messagingMock),
  };
});

// No need to export the mocked Firebase - Jest's hoisting will take care of it
console.log('Firebase mock setup complete'); 