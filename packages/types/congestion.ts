export type CongestionLevel = 'GREEN' | 'YELLOW' | 'ORANGE' | 'RED';

export interface CongestionData {
    areaName: string;
    congestionLevel: CongestionLevel;
    congestionMessage: string;
    populationMin: number;
    populationMax: number;
    updateTime: string;
}