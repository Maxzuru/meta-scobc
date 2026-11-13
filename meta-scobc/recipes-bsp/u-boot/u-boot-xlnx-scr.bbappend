# The generic Xilinx boot script expects the distro-boot framework to set
# these variables. After the QSPI boot, U-Boot sources the boot.scr preloaded
# by runqemu without them, so select the OS boot partition explicitly.
SCOBC_QEMU_OS_DEVTYPE ??= "mmc"
SCOBC_QEMU_OS_DEVNUM ??= "0"
SCOBC_QEMU_OS_DEVNUM:scobc-qemu-os-emmc = "1"
SCOBC_QEMU_OS_BOOT_PART ??= "1"

PRE_BOOTENV:scobc-qemu-boot-qspi = "setenv devtype ${SCOBC_QEMU_OS_DEVTYPE}; setenv devnum ${SCOBC_QEMU_OS_DEVNUM}; setenv distro_bootpart ${SCOBC_QEMU_OS_BOOT_PART}; setenv boot_targets ${SCOBC_QEMU_OS_DEVTYPE}${SCOBC_QEMU_OS_DEVNUM};"
