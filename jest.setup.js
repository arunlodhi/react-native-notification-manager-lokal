import "react-native-gesture-handler/jestSetup";

jest.mock("react-native-reanimated", () => {
  const Reanimated = require("react-native-reanimated/mock");
  Reanimated.default.call = () => {};
  return Reanimated;
});

// Mock the native module
jest.mock("react-native", () => {
  const RN = jest.requireActual("react-native");

  RN.NativeModules.NotificationManagerModule = {
    initialize: jest.fn(() => Promise.resolve(true)),
    createNotification: jest.fn(() => Promise.resolve(true)),
    createNotificationWithImage: jest.fn(() => Promise.resolve(true)),
    createCricketNotification: jest.fn(() => Promise.resolve(true)),
    createQuizNotification: jest.fn(() => Promise.resolve(true)),
    createCommentNotification: jest.fn(() => Promise.resolve(true)),
    cancelNotification: jest.fn(() => Promise.resolve(true)),
    refreshNotifications: jest.fn(() => Promise.resolve(0)),
    limitNotifications: jest.fn(() => Promise.resolve(0)),
    getActiveNotifications: jest.fn(() => Promise.resolve([])),
    getMatrimonyNotificationType: jest.fn(() => Promise.resolve(null)),
    setSilentPush: jest.fn(() => Promise.resolve(true)),
    setNotificationVersion: jest.fn(() => Promise.resolve(true)),
    scheduleNotificationRefresh: jest.fn(() => Promise.resolve(true)),
    cancelNotificationRefresh: jest.fn(() => Promise.resolve(true)),
    getStoredIntArray: jest.fn(() => Promise.resolve([])),
    storeIntArray: jest.fn(() => Promise.resolve(true)),
    areNotificationsEnabled: jest.fn(() => Promise.resolve(true)),
  };

  return RN;
});

// Silence the warning: Animated: `useNativeDriver` is not supported
jest.mock("react-native/Libraries/Animated/NativeAnimatedHelper");
