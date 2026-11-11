SUMMARY = "Prebuilt lockstep RPU firmware for SC-OBC Module V1"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

RPU_FIRMWARE_URI ??= ""
RPU_FIRMWARE_SHA256 ??= "${@ \
    d.getVarFlag('RPU_FIRMWARE_URI', 'sha256sum') or '' \
}"

RPU_FIRMWARE_SRC_URI = "${@'%s;name=rpu-firmware' % d.getVar('RPU_FIRMWARE_URI') if d.getVar('RPU_FIRMWARE_URI') else ''}"

SRC_URI += " ${RPU_FIRMWARE_SRC_URI}"
SRC_URI[rpu-firmware.sha256sum] = "${RPU_FIRMWARE_SHA256}"

S = "${WORKDIR}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_DEFAULT_DEPS = "1"
INHIBIT_SYSROOT_STRIP = "1"

# The ELF is consumed by bootgen through the recipe sysroot, not installed
# in the target root filesystem.
PACKAGES = ""
do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -Dm 0644 "${RPU_FIRMWARE_UNPACKED_PATH}" \
        "${D}${datadir}/rpu-firmware/rpu-firmware.elf"
}

python __anonymous() {
    if not d.getVar("RPU_FIRMWARE_URI"):
        raise bb.parse.SkipRecipe("RPU_FIRMWARE_URI must be specified")

    uri = d.getVar('RPU_FIRMWARE_SRC_URI')
    scheme, _, path, _, _, params = bb.fetch2.decodeurl(uri)
    # do_unpack preserves directory components for file:// URIs, while
    # downloaded files use their local basename (including downloadfilename).
    if scheme == 'file':
        filename = path.lstrip('/')
    else:
        filename = os.path.basename(bb.fetch2.localpath(uri, d))
    d.setVar('RPU_FIRMWARE_UNPACKED_PATH',
             os.path.join('${WORKDIR}', params.get('subdir', ''), filename))
}
