SHINKAI_BUILD_DATE := $(shell date -u +%Y%m%d-%H%M)

SHINKAI_PLATFORM_VERSION := heptakaideka

SHINKAI_VERSION := $(CUSTOM_BUILD)-$(SHINKAI_PLATFORM_VERSION)-$(SHINKAI_BUILD_DATE)
SHINKAI_VERSION_PROP := seventeen

# Additional Flags
PERF_ANIM_OVERRIDE ?= false

# Shinkai / Palapa Platform Version
PRODUCT_PRODUCT_PROPERTIES += \
    ro.shinkai.build.date=$(SHINKAI_BUILD_DATE) \
    ro.custom.device=$(CUSTOM_BUILD) \
    ro.shinkai.maintainer=$(SHINKAI_MAINTAINER) \
    ro.shinkai.version=$(SHINKAI_PLATFORM_VERSION)-$(CUSTOM_BUILD)-$(SHINKAI_BUILD_DATE) \
    ro.palapa.edition=Majapahit Edition \
    ro.palapa.os.name=Palapa OS \
    ro.palapa.os.version=Majapahit \
    ro.wowo.edition=Majapahit Edition \
    ro.wowo.os.name=Palapa OS \
    ro.wowo.os.version=Majapahit \
    net.shinkai.version=$(SHINKAI_VERSION_PROP)

# Updater
    PRODUCT_PRODUCT_PROPERTIES += \
        net.shinkai.build_type=ci

# Anim Override
PRODUCT_PRODUCT_PROPERTIES += \
    persist.sys.activity_anim_perf_override=$(PERF_ANIM_OVERRIDE)
