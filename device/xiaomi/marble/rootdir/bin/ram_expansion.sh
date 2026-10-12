#!/vendor/bin/sh
#
# Copyright (C) 2026 Shinkai Project / Wowo OS
# RAM Expansion / zRAM Controller
#

ramExpansion=`getprop persist.vendor.ram_expansion`
if [ -z "$ramExpansion" ]; then
    ramExpansion=`getprop vendor.ram_expansion`
fi
if [ -z "$ramExpansion" ]; then
    ramExpansion=`getprop persist.sys.ram_expansion`
fi

diskSizeUnit=M

if [ "$ramExpansion" = "0" ] || [ "$ramExpansion" = "off" ]; then
    zRamSizeMB=0
elif [ -n "$ramExpansion" ] && [ "$ramExpansion" -gt 0 ] 2>/dev/null; then
    zRamSizeMB=$((ramExpansion * 1024))
else
    # Default 4GB (4096MB)
    zRamSizeMB=4096
fi

# Always swapoff and reset zram0 first to release existing memory and allow re-sizing
swapoff /dev/block/zram0 2>/dev/null
echo 1 > /sys/block/zram0/reset 2>/dev/null

if [ "$zRamSizeMB" -eq 0 ]; then
    exit 0
fi

echo lz4 > /sys/block/zram0/comp_algorithm

if [ -f /sys/block/zram0/disksize ]; then
    if [ -f /sys/block/zram0/use_dedup ]; then
        echo 1 > /sys/block/zram0/use_dedup
    fi
    echo "$zRamSizeMB""$diskSizeUnit" > /sys/block/zram0/disksize

    if [ -e /sys/kernel/slab/zs_handle ]; then
        echo 0 > /sys/kernel/slab/zs_handle/store_user
    fi
    if [ -e /sys/kernel/slab/zspage ]; then
        echo 0 > /sys/kernel/slab/zspage/store_user
    fi

    mkswap /dev/block/zram0
    swapon /dev/block/zram0 -p 32758
fi

echo 100 > /proc/sys/vm/swappiness
exit 0
