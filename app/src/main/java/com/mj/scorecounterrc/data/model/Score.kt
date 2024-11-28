package com.mj.scorecounterrc.data.model

data class Score(var left: Int, var right: Int) {

}

enum class ScoreSide {
    LEFT,
    RIGHT
}