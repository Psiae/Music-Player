package com.flammky.musicplayer.domain.musiclib.player.exoplayer.stream

import androidx.media3.exoplayer.upstream.Allocation
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator

abstract class LibAllocator : Allocator {
	private val todoImpl = DefaultAllocator(true, 64 * 1024)
	override fun allocate(): Allocation = todoImpl.allocate()
	override fun release(allocation: Allocation) = todoImpl.release(allocation)
	override fun release(allocationNode: Allocator.AllocationNode) = todoImpl.release(allocationNode)
	override fun trim() = todoImpl.trim()
	override fun getTotalBytesAllocated(): Int = todoImpl.totalBytesAllocated
	override fun getIndividualAllocationLength(): Int = todoImpl.individualAllocationLength
}
