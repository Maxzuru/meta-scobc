# QEMU model of the SC-OBC Module V1, used by runqemu

# QEMU hardware device trees
QEMU_HW_DTB_PS ?= "${QEMU_HW_DTB_PATH}/board-scobc-v1-ps-virt.dtb"
QEMU_HW_DTB_PMC ?= "${QEMU_HW_DTB_PATH}/board-versal-xcve2302-pmc-virt.dtb"

# Boot (stage 1) source: sd, emmc or qspi
SCOBC_QEMU_BOOT_SOURCE ??= "sd"
# Source of the OS image: sd or emmc
SCOBC_QEMU_OS_SOURCE ??= "sd"

MACHINEOVERRIDES .= ":scobc-qemu-boot-${SCOBC_QEMU_BOOT_SOURCE}:scobc-qemu-os-${SCOBC_QEMU_OS_SOURCE}"

# The board has 4 GB of DDR memory
QB_MEM ?= "-m 4G"

# Boot from SD controller 0 like the real hardware, instead of boot mode 5
# which uses SD controller 1 in 2.0 mode
QEMU_HW_BOOT_MODE ?= "3"
QEMU_HW_BOOT_MODE:scobc-qemu-boot-emmc = "6"
QEMU_HW_BOOT_MODE:scobc-qemu-boot-qspi = "2"

QEMU_HW_SD_DRIVE_INDEX ?= "0"
QEMU_HW_SD_DRIVE_INDEX:scobc-qemu-os-emmc = "3"

# Both QSPI flashes read the padded boot image from xilinx-bootbin
QB_OPT_APPEND:append:scobc-qemu-boot-qspi = " \
    -drive file=@DEPLOY_DIR_IMAGE@/qemu-qspi-${MACHINE}.bin,if=mtd,format=raw,index=0,readonly=on \
    -drive file=@DEPLOY_DIR_IMAGE@/qemu-qspi-${MACHINE}.bin,if=mtd,format=raw,index=1,readonly=on"
