package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class CueSchedulerTest {

    private val kilometre = UnitSystem.METRIC.cueUnit
    private val mile = UnitSystem.IMPERIAL.cueUnit

    @Test
    fun `nothing is announced before the first milestone`() {
        assertEquals(
            emptyList<Int>(),
            CueScheduler.milestonesCrossed(Distance(0.0), Distance(999.0), kilometre),
        )
    }

    @Test
    fun `crossing a kilometre announces it once`() {
        assertEquals(
            listOf(1),
            CueScheduler.milestonesCrossed(Distance(999.0), Distance(1_001.0), kilometre),
        )
    }

    @Test
    fun `landing exactly on a milestone still announces it`() {
        assertEquals(
            listOf(1),
            CueScheduler.milestonesCrossed(Distance(999.0), Distance(1_000.0), kilometre),
        )
    }

    @Test
    fun `a milestone is never announced twice`() {
        assertEquals(
            emptyList<Int>(),
            CueScheduler.milestonesCrossed(Distance(1_001.0), Distance(1_500.0), kilometre),
        )
    }

    @Test
    fun `recomputing the same distance announces nothing`() {
        assertEquals(
            emptyList<Int>(),
            CueScheduler.milestonesCrossed(Distance(2_000.0), Distance(2_000.0), kilometre),
        )
    }

    @Test
    fun `a gap that spans several milestones announces each of them in order`() {
        assertEquals(
            listOf(1, 2, 3),
            CueScheduler.milestonesCrossed(Distance(500.0), Distance(3_200.0), kilometre),
        )
    }

    @Test
    fun `miles are announced on their own boundaries`() {
        assertEquals(
            emptyList<Int>(),
            CueScheduler.milestonesCrossed(Distance(999.0), Distance(1_001.0), mile),
        )
        assertEquals(
            listOf(1),
            CueScheduler.milestonesCrossed(Distance(1_600.0), Distance(1_610.0), mile),
        )
    }

    @Test
    fun `a half unit interval announces twice as often`() {
        val halfKilometre = Distance(500.0)

        assertEquals(
            listOf(1, 2),
            CueScheduler.milestonesCrossed(Distance(400.0), Distance(1_050.0), halfKilometre),
        )
    }

    @Test
    fun `distance never runs backwards but the guard holds anyway`() {
        assertEquals(
            emptyList<Int>(),
            CueScheduler.milestonesCrossed(Distance(5_000.0), Distance(1_000.0), kilometre),
        )
    }

    @Test
    fun `a zero interval announces nothing instead of looping forever`() {
        assertEquals(
            emptyList<Int>(),
            CueScheduler.milestonesCrossed(Distance(0.0), Distance(10_000.0), Distance.Zero),
        )
    }
}
