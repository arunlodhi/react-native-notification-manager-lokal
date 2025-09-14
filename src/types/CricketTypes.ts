/**
 * Cricket Notification Types
 * Matches the exact Android cricket notification implementation
 */

export interface CricketTeam {
  id: string;
  name: string;
  shortName: string;
  iconUrl?: string;
}

export interface CricketInnings {
  score: string;
  wickets: string;
  overs: string;
}

export interface CricketTeamWithInnings extends CricketTeam {
  innings: CricketInnings[];
}

export interface CricketMatch {
  id: string;
  team1: CricketTeam;
  team2: CricketTeam;
  batTeam: CricketTeamWithInnings;
  bowTeam: CricketTeamWithInnings;
  venue: {
    name: string;
    city?: string;
  };
  header: {
    status: string;
    matchType: string;
  };
  imageUrl: string;
  matchState: CricketMatchState;
}

export enum CricketMatchState {
  PREVIEW = "PREVIEW",
  INPROGRESS = "INPROGRESS",
  COMPLETE = "COMPLETE",
  DEFAULT = "DEFAULT",
}

export interface CricketNotificationData {
  matchState: CricketMatchState;
  team1Name: string;
  team2Name: string;
  team1ShortName: string;
  team2ShortName: string;
  team1IconUrl?: string;
  team2IconUrl?: string;
  matchStatus?: string;
  venue?: string;
  team1Score?: string;
  team1Wickets?: string;
  team1Overs?: string;
  team2Score?: string;
  team2Wickets?: string;
  team2Overs?: string;
}

export interface CricketNotificationCallbacks {
  onNotificationBuilt: (bundle: { [key: string]: any }) => void;
}
