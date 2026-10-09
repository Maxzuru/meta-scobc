SUMMARY = "NASA core Flight System (cFS) for the SC-OBC Module V1 APU"
DESCRIPTION = "cFE, OSAL, PSP and cFS applications, built for Linux with \
the cFE mission build. The mission build runs on the build host and builds \
the host tools, such as elf2cfetbl. It configures one cross build per CPU, \
which uses the toolchain of this build."
HOMEPAGE = "https://github.com/nasa/cFS"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = " \
    file://cfe/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://osal/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://psp/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://apps/ci_lab/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://apps/sample_app/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://apps/sch_lab/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://apps/to_lab/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://libs/sample_lib/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://tools/elf2cfetbl/LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    "

# The revisions match the Zephyr build of the mission (west.yml in
# cfs-demo). The Space Cubics cFE and PSP forks only add the Zephyr port on
# top of the upstream revisions used here.
SRC_URI = " \
    git://github.com/nasa/cFE.git;protocol=https;branch=dev;name=cfe;destsuffix=cfs/cfe \
    git://github.com/Maxzuru/osal-zephyr.git;protocol=https;branch=zephyr-functional-tests;name=osal;destsuffix=cfs/osal \
    git://github.com/nasa/PSP.git;protocol=https;branch=dev;name=psp;destsuffix=cfs/psp \
    git://github.com/nasa/ci_lab.git;protocol=https;branch=main;name=ci_lab;destsuffix=cfs/apps/ci_lab \
    git://github.com/nasa/sample_app.git;protocol=https;branch=main;name=sample_app;destsuffix=cfs/apps/sample_app \
    git://github.com/nasa/sch_lab.git;protocol=https;branch=main;name=sch_lab;destsuffix=cfs/apps/sch_lab \
    git://github.com/nasa/to_lab.git;protocol=https;branch=main;name=to_lab;destsuffix=cfs/apps/to_lab \
    git://github.com/nasa/sample_lib.git;protocol=https;branch=main;name=sample_lib;destsuffix=cfs/libs/sample_lib \
    git://github.com/nasa/elf2cfetbl.git;protocol=https;branch=main;name=elf2cfetbl;destsuffix=cfs/tools/elf2cfetbl \
    ${CFS_MISSION_URI};name=mission;destsuffix=cfs/mission \
    file://tools/CMakeLists.txt;subdir=cfs \
    file://cfs.service \
    "

# Mission repository, which holds the <mission>_defs directory
CFS_MISSION_URI ?= "git://git@github.com/spacecubics/scobc-mission.git;protocol=ssh;branch=main"

SRCREV_cfe = "f22e538cafe56cf44b15996b67bd1e90850a30cf"
SRCREV_osal = "2da81c1c60129209f44e1f42e079f79a6cf6becc"
SRCREV_psp = "53df1d54c3d29fd23832c59df1104de07a043af2"
SRCREV_ci_lab = "f5d36625336249312ee9d5815bc875e231815bb4"
SRCREV_sample_app = "2f93d1a4159a02b18d67ee83342c9e96b90e23e4"
SRCREV_sch_lab = "bc848cd8a6c0407023b959191e8aa39a01928444"
SRCREV_to_lab = "38f7312ec4c1109b8f1c0738730b6e5ac5860f05"
SRCREV_sample_lib = "2d8b7e862673ef90da4355890e18e92822e8acd7"
SRCREV_elf2cfetbl = "118b55fe1d128b48dc45fd36278b87827c6d3faa"
SRCREV_mission = "ceb8097070a975a52712a4f8b20991f91eca44b4"
SRCREV_FORMAT = "cfe"

PV = "7.0.1+git"

S = "${WORKDIR}/cfs"

inherit cmake systemd

# Mission definitions directory (<mission>_defs in the mission repository)
# and the CPU of the mission that runs on this machine.
CFS_MISSION ?= "scobc"
CFS_CPUNAME ?= "apu"
CFS_INSTALL_DIR ?= "/opt/cfs"

# Toolchain name that <cpu>_SYSTEM refers to in targets.cmake
CFS_TOOLCHAIN ?= "yocto"

# cFE records the git version of each module and the build environment in
# the image. Keep git from looking at repositories outside the work
# directory, and make the build environment reproducible.
export GIT_CEILING_DIRECTORIES = "${WORKDIR}"
export HOSTNAME = "yocto"
export USER = "yocto"

do_configure() {
    # The cross build of each CPU uses the toolchain file of cmake.bbclass
    # and the Linux PSP and OSAL. cFE looks for toolchain files in the
    # mission definitions first, then in its own cmake directory.
    cat > ${S}/cfe/cmake/toolchain-${CFS_TOOLCHAIN}.cmake <<EOF
include(${WORKDIR}/toolchain.cmake)
set(CFE_SYSTEM_PSPNAME pc-linux)
set(OSAL_SYSTEM_OSTYPE posix)
EOF

    # The mission build is a host build, it runs the host tools it builds.
    # Override the compiler and flags of the target environment.
    cmake \
        -G "Unix Makefiles" \
        -S ${S}/cfe \
        -B ${B} \
        -DMISSIONCONFIG=${CFS_MISSION} \
        -DMISSION_DEFS=${S}/mission/${CFS_MISSION}_defs \
        -DCMAKE_INSTALL_PREFIX=${CFS_INSTALL_DIR} \
        -DCMAKE_C_COMPILER="${@d.getVar('BUILD_CC').split()[-1]}" \
        -DCMAKE_C_FLAGS="${BUILD_CFLAGS}" \
        -DCMAKE_EXE_LINKER_FLAGS="${BUILD_LDFLAGS}" \
        -Wno-dev
}

do_compile() {
    export BUILDDATE="$(date -u -d @${SOURCE_DATE_EPOCH} +%Y%m%d-%H%M)"
    oe_runmake -C ${B} mission-all
}

do_install() {
    oe_runmake -C ${B} DESTDIR=${D} mission-install

    # Host tools of the mission build
    rm -rf ${D}${CFS_INSTALL_DIR}/host

    install -d ${D}${systemd_system_unitdir}
    sed -e 's|@CFS_INSTALL_DIR@|${CFS_INSTALL_DIR}|g' \
        -e 's|@CFS_CPUNAME@|${CFS_CPUNAME}|g' \
        ${WORKDIR}/cfs.service > ${D}${systemd_system_unitdir}/cfs.service
    chmod 0644 ${D}${systemd_system_unitdir}/cfs.service
}

FILES:${PN} += "${CFS_INSTALL_DIR}"

SYSTEMD_SERVICE:${PN} = "cfs.service"
SYSTEMD_AUTO_ENABLE:${PN} ?= "enable"
