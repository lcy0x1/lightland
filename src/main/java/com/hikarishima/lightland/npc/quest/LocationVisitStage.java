package com.hikarishima.lightland.npc.quest;

import com.hikarishima.lightland.npc.player.PlayerProgress;
import com.hikarishima.lightland.npc.token.LocationVisitToken;
import com.hikarishima.lightland.npc.token.QuestToken;
import com.lcy0x1.core.util.SerialClass;

@SerialClass
public class LocationVisitStage extends IQuestStage {

    @SerialClass.SerialField
    public double x, y, z, r;

    @Override
    public QuestToken genToken(PlayerProgress progress) {
        return new LocationVisitToken().init(progress);
    }
}
