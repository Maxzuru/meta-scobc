SUMMARY = "SC-OBC V1 safety processor firmware for QEMU"
DESCRIPTION = "Deploys the safety processor firmware loaded by the riscv32 \
QEMU instance that runs alongside the Versal model."
LICENSE = "CLOSED"

SP_FIRMWARE_URI ??= ""
SP_FIRMWARE_SHA256 ??= "${@ \
    d.getVarFlag('SP_FIRMWARE_URI', 'sha256sum') or '' \
}"

SP_FIRMWARE_SRC_URI = "${@'%s;name=sp-firmware' % d.getVar('SP_FIRMWARE_URI') if d.getVar('SP_FIRMWARE_URI') else ''}"

SRC_URI += " ${SP_FIRMWARE_SRC_URI}"
SRC_URI[sp-firmware.sha256sum] = "${SP_FIRMWARE_SHA256}"

S = "${WORKDIR}"
COMPATIBLE_MACHINE = "scobc-v1-generic"
PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_DEFAULT_DEPS = "1"

inherit deploy nopackages

do_configure[noexec] = "1"
do_compile[noexec] = "1"
do_install[noexec] = "1"

# QEMU loads the ELF from the deploy directory. Its generic loader detects
# the image format from the file content, so the deployed name does not
# depend on the fetched file.
do_deploy() {
    install -Dm 0644 "${SP_FIRMWARE_UNPACKED_PATH}" \
        "${DEPLOYDIR}/sp-firmware.elf"
}
addtask deploy after do_install before do_build

python __anonymous() {
    if not d.getVar("SP_FIRMWARE_URI"):
        raise bb.parse.SkipRecipe("SP_FIRMWARE_URI must be specified")

    uri = d.getVar('SP_FIRMWARE_SRC_URI')
    scheme, _, path, _, _, params = bb.fetch2.decodeurl(uri)
    # do_unpack preserves directory components for file:// URIs, while
    # downloaded files use their local basename (including downloadfilename).
    if scheme == 'file':
        filename = path.lstrip('/')
    else:
        filename = os.path.basename(bb.fetch2.localpath(uri, d))
    d.setVar('SP_FIRMWARE_UNPACKED_PATH',
             os.path.join('${WORKDIR}', params.get('subdir', ''), filename))
}
