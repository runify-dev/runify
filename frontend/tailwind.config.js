/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./ index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            zIndex: {
                '9999': '9999',
                '99999': '99999'
            }
        },
    },
    plugins: [],
}