# Add a lockstep RPU firmware ELF to the Versal application image.
RPU_FIRMWARE_IMAGE_ID ?= "0x1c000000"

# xilinx-bootbin adds the partition names to DEPENDS automatically.
BIF_RPU_PARTITIONS:append = " rpu-firmware"

BIF_PARTITION_ATTR[rpu-firmware] = "core=r5-lockstep"
BIF_PARTITION_IMAGE[rpu-firmware] = "${RECIPE_SYSROOT}${datadir}/rpu-firmware/rpu-firmware.elf"
BIF_PARTITION_ID[rpu-firmware] = "${RPU_FIRMWARE_IMAGE_ID}"
