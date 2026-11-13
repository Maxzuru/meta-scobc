# QEMU model of the SC-OBC Module V1, used by runqemu

# QEMU hardware device trees
QEMU_HW_DTB_PS ?= "${QEMU_HW_DTB_PATH}/board-scobc-v1-ps-virt.dtb"
QEMU_HW_DTB_PMC ?= "${QEMU_HW_DTB_PATH}/board-versal-xcve2302-pmc-virt.dtb"
# The safety processor reports the board revision and grade
QEMU_HW_DTB_SP ?= "${QEMU_HW_DTB_PATH}/board-scobc-v1-sp-virt.dtb"
QEMU_HW_DTB_SP:scobc-v1-reva = "${QEMU_HW_DTB_PATH}/board-scobc-v1-reva-sp-virt.dtb"
QEMU_HW_DTB_SP:scobc-v1-reva:scobc-v1-devgrade = "${QEMU_HW_DTB_PATH}/board-scobc-v1-devgrade-reva-sp-virt.dtb"

# Boot (stage 1) source: sd, emmc or qspi
SCOBC_QEMU_BOOT_SOURCE ??= "sd"
# Source of the OS image: sd or emmc
SCOBC_QEMU_OS_SOURCE ??= "sd"

# Run the safety processor alongside the Versal model
ENABLE_SP_EMULATION ??= "0"

MACHINEOVERRIDES .= ":scobc-qemu-boot-${SCOBC_QEMU_BOOT_SOURCE}:scobc-qemu-os-${SCOBC_QEMU_OS_SOURCE}"
MACHINEOVERRIDES .= "${@':scobc-qemu-sp' if bb.utils.to_boolean(d.getVar('ENABLE_SP_EMULATION'), False) else ''}"

# The board has 4 GB of DDR memory
QB_MEM ?= "-m 4G"

# The RPU firmware bundled into boot.bin prints to the second PL011. QEMU
# does not wait for a connection, so output before connecting is lost.
# Use a chardev, as the multiarch wrapper swaps "tcp:127.0.0.1:" arguments
# with the runqemu slirp network options. The hardware device tree binds
# the UART to the chardev named serial3.
SCOBC_QEMU_RPU_SERIAL ?= "-chardev socket,id=serial3,host=127.0.0.1,port=4321,server=on,wait=off -serial chardev:serial3"
QEMU_HW_SERIAL ?= "-serial null -serial null -serial mon:stdio ${@ \
    d.getVar('SCOBC_QEMU_RPU_SERIAL') \
    if bb.utils.to_boolean(d.getVar('ENABLE_RPU_FIRMWARE'), False) \
    else '-serial null'}"

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

# Run the safety processor QEMU alongside the Versal instances and connect
# it to SPI1 over remote-port. The multiarch wrapper starts the riscv32
# instance given with -asu-args in the common machine path, where both
# sides find the remote-port socket.
SCOBC_QEMU_SP_SERIAL ?= "-serial tcp:127.0.0.1:4322,server=on,wait=off"
# Human monitor for qom-set on the sensor models
SCOBC_QEMU_SP_MONITOR ?= "-monitor tcp:127.0.0.1:4323,server=on,wait=off"

QEMU_HW_DTB_PS:scobc-qemu-sp = "${QEMU_HW_DTB_PATH}/board-scobc-v1-ps-sp-virt.dtb"
# The safety processor resets the whole Versal, so the PMC instance has to
# reset with the main processor instance
QEMU_HW_DTB_PMC:scobc-qemu-sp = "${QEMU_HW_DTB_PATH}/board-scobc-v1-pmc-sp-virt.dtb"

EXTRA_IMAGEDEPENDS:append:scobc-qemu-sp = " sp-firmware"

QB_OPT_APPEND:append:scobc-qemu-sp = " -asu-args '-M riscv-fdt \
    -hw-dtb @DEPLOY_DIR_IMAGE@/${QEMU_HW_DTB_SP} \
    -device loader,file=@DEPLOY_DIR_IMAGE@/sp-firmware.elf,cpu-num=0 \
    -nodefaults -display none ${SCOBC_QEMU_SP_SERIAL} ${SCOBC_QEMU_SP_MONITOR}'"
