module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/tests'],
  testMatch: ['**/*.test.ts'],
  collectCoverage: true,
  coverageDirectory: 'coverage',
  collectCoverageFrom: [
    'controllers/**/*.ts',
    'routes/**/*.ts',
    '!**/node_modules/**',
    '!**/vendor/**'
  ],
  coverageReporters: ['text', 'lcov', 'clover', 'html'],
  verbose: true,
  testTimeout: 3000,
  setupFiles: ['dotenv/config'],
  setupFilesAfterEnv: ['<rootDir>/tests/setupFirebaseMock.ts'],
  forceExit: true,
  detectOpenHandles: true
}; 