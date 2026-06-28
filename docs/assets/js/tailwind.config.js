tailwind.config = {
    theme: {
        extend: {
            colors: {
                'background-sand': '#F4F3F0',
                'text-main': '#1A1D24',
                'blueprint-navy': '#2C3A47',
                'primary': '#D94532',
                'offwhite': '#F4F3F0',
                'lightgray': '#E5E7EB',
                'charcoal': '#1A1D24',
                'steel': '#4B5E82',
                'skyblue': '#2563EB',
                'purewhite': '#FFFFFF',
                'forest': '#059669',
                'danger': '#DC2626'
            },
            fontFamily: {
                mono: ['JetBrains Mono', 'monospace'],
                primary:   ['"JetBrains Mono"', 'monospace'],
                secondary: ['ui-monospace', '"Courier New"', 'monospace'],
            },
            fontSize: {
                'xs':             ['12px', '16px'],
                'sm':             ['14px', '20px'],
                'base':           ['16px', '24px'],
                'lg':             ['18px', '28px'],
                'display-mobile': ['32px', '32px'],
                'display-tablet': ['48px', '48px'],
            }
        }
    }
};
