define loadrpu
	set pagination off
	set confirm off

	target extended-remote localhost:1234

	add-inferior
	inferior 2
	attach 2

	file $arg0

	thread 2.1
	maintenance packet Qqemu.PhyMemMode:0

	# Configure R5 lockstep/TCM and hold the RPU in reset.
	set {unsigned int}0xff9a0000 = 0x50
	set {unsigned int}0xff9a0100 = 0x1
	set {unsigned int}0xff5e0300 = 0x17

	load

	# Release reset and start at the Zephyr entry point.
	set {unsigned int}0xff5e0300 = 0x14
	set $pc = __start

	set schedule-multiple on
	continue
end
