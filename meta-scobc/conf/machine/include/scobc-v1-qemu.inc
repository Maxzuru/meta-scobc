# QEMU model of the SC-OBC Module V1, used by runqemu

# QEMU hardware device trees
QEMU_HW_DTB_PS ?= "${QEMU_HW_DTB_PATH}/board-scobc-v1-ps-virt.dtb"
QEMU_HW_DTB_PMC ?= "${QEMU_HW_DTB_PATH}/board-versal-xcve2302-pmc-virt.dtb"

# The board has 4 GB of DDR memory
QB_MEM ?= "-m 4G"

# Boot from SD controller 0 like the real hardware, instead of boot mode 5
# which uses SD controller 1 in 2.0 mode
QEMU_HW_BOOT_MODE ?= "3"
QEMU_HW_SD_DRIVE_INDEX ?= "0"
