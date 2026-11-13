DESCRIPTION = "Bootable RAM-root SD image for SC-OBC Module V1"
LICENSE = "MIT"

inherit core-image
inherit kernel-artifact-names

IMAGE_NAME_SUFFIX ?= ""

# This recipe only assembles the bootable WIC image.
# The actual root filesystem is built by INITRAMFS_IMAGE.
IMAGE_INSTALL = ""
IMAGE_LINGUAS = " "

IMAGE_FSTYPES = "wic wic.xz wic.bmap"
WKS_FILES = "sc-ramroot-sd.wks"

# Provide the padded SD image for runqemu
IMAGE_FSTYPES += "wic.qemu-sd"
QB_DEFAULT_FSTYPE = "wic.qemu-sd"
QB_FSINFO = "wic:no-kernel-in-fs"
QB_ROOTFS = ""
QB_ROOTFS_OPT = " -drive if=sd,index=${QEMU_HW_SD_DRIVE_INDEX},file=@ROOTFS@,format=raw"

# PetaLinux normally places the plain fitImage in IMAGE_BOOT_FILES.
# Replace it with the artifact appropriate for the selected mode.
IMAGE_BOOT_FILES:remove = "fitImage"
IMAGE_BOOT_FILES:append = " \
    fitImage-${INITRAMFS_IMAGE_NAME}-${KERNEL_FIT_LINK_NAME};image.ub \
"

do_image_wic[depends] += " virtual/kernel:do_deploy"
