#!/vendor/bin/sh
#
# Copyright (C) 2026 Shinkai Project / Wowo OS
# RAM Expansion / zRAM Controller (FurLC ZRAM Method)
#

ramExpansion=`getprop persist.vendor.ram_expansion`
if [ -z "$ramExpansion" ]; then
    # Default: 4GB expansion
    ramExpansion="4"
fi

if [ "$ramExpansion" = "0" ] || [ "$ramExpansion" = "off" ]; then
    zRamSizeMB=0
elif [ "$ramExpansion" = "2" ]; then
    zRamSizeMB=2048
elif [ "$ramExpansion" = "4" ]; then
    zRamSizeMB=4096
elif [ "$ramExpansion" = "8" ]; then
    zRamSizeMB=8192
elif [ "$ramExpansion" = "16" ]; then
    zRamSizeMB=16384
elif [ -n "$ramExpansion" ] && [ "$ramExpansion" -gt 0 ] 2>/dev/null; then
    let zRamSizeMB="$ramExpansion * 1024"
else
    # Default 4096MB
    zRamSizeMB=4096
fi

# 1. Swapoff zram0 if active
swapoff /dev/block/zram0 2>/dev/null
sleep 1

# 2. If target is off (0), reset and set swappiness to 0
if [ "$zRamSizeMB" -eq 0 ]; then
    echo 1 > /sys/block/zram0/reset 2>/dev/null
    echo 0 > /proc/sys/vm/swappiness 2>/dev/null
    exit 0
fi

# 3. Reset zram0 with retry loop
for i in 1 2 3 4 5; do
    echo 1 > /sys/block/zram0/reset 2>/dev/null && break
    sleep 1
done

# 4. Multi-stream and compression algorithm
if [ -f /sys/block/zram0/max_comp_streams ]; then
    echo 8 > /sys/block/zram0/max_comp_streams 2>/dev/null
fi
if [ -f /sys/block/zram0/comp_algorithm ]; then
    echo lz4 > /sys/block/zram0/comp_algorithm 2>/dev/null
fi

# 5. Set disksize in Megabytes unit (M) supported by kernel memparse
if [ -f /sys/block/zram0/disksize ]; then
    if [ -f /sys/block/zram0/use_dedup ]; then
        echo 1 > /sys/block/zram0/use_dedup 2>/dev/null
    fi
    echo "${zRamSizeMB}M" > /sys/block/zram0/disksize

    if [ -e /sys/kernel/slab/zs_handle ]; then
        echo 0 > /sys/kernel/slab/zs_handle/store_user 2>/dev/null
    fi
    if [ -e /sys/kernel/slab/zspage ]; then
        echo 0 > /sys/kernel/slab/zspage/store_user 2>/dev/null
    fi

    mkswap /dev/block/zram0
    swapon /dev/block/zram0 -p 32758
fi

echo 100 > /proc/sys/vm/swappiness 2>/dev/null
exit 0

