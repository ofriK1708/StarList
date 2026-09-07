# 🌟 StarList

**Turn Your Tasks into Stars, Build Your Galaxy!**

StarList is a gamified productivity application that integrates task management and habit tracking with a visual motivational page, "Your Galaxy." It uniquely rewards users with virtual coins, proportional to a task's difficulty, which are then used to build out their personalized galaxy, maintaining long-term engagement. It also offers an AI chat feature to assist users in creating and defining tasks and habits

---

## ✨ Features

### 📋 Task Management
- Create, organize, and prioritize your tasks with ease
- Set difficulty levels for each task
- Track completion progress in real-time
- Categorize tasks for better organization

### 🎯 Habit Tracking
- Build and maintain positive habits
- Set daily, weekly, or custom habit goals
- Monitor your streaks and consistency
- Get insights into your habit patterns

### 🌌 Your Galaxy - Visual Motivation
- **Unique Reward System**: Earn virtual coins based on task difficulty
- **Build Your Universe**: Use earned coins to expand and customize your personal galaxy
- **Visual Progress**: Watch your galaxy grow as you complete tasks and build habits
- **Long-term Engagement**: Stay motivated with an ever-evolving visual representation of your productivity journey
- **Start each month anew**: see how each month you build your galaxy, and see how it compares with other months

### 💰 Coin Reward System
- **Difficulty-Based Rewards**: Harder tasks = more coins
- **Fair Compensation**: Earn rewards proportional to your effort
- **Spend Wisely**: Use coins to unlock new stars, planets, and cosmic elements
- **Endless Possibilities**: Continuously expand your galaxy as you stay productive

### ✨ AI Capabilities: 
- Assist users in creating and defining tasks and habits
- Determining their difficulty and duration
- Tracking progress and improvements through discussions
- Daily briefings and gentle, fact-based coach notices that nudge you toward habits that are due or overdue, without penalizing missed days

### 📊 Statistics Dashboard:
- Provides charts to track progress, patterns, and trends across tasks and habits.

### 📅 Calendar Overview
- Offers a view of monthly performance, completion rates, and consistency.

---

## 🚀 Getting Started

### Prerequisites
- Java 25 and Maven (for `StarList-Backend`)
- Node.js and pnpm/npm (for `frontend`)
- A PostgreSQL database
- An OpenAI API key (for the AI chat feature)

### Installation
1. **Backend** — from `StarList-Backend/`, configure your database and OpenAI credentials in `app/src/main/resources/application-dev.yml`, then run:
   ```bash
   mvn spring-boot:run -pl app
   ```
2. **Frontend** — from `frontend/`, install dependencies and start the dev server:
   ```bash
   npm install
   npm run dev
   ```

## 📖 Usage

### Creating Your First Task
1. Launch StarList
2. Click on "New Task"
3. Enter task details and set difficulty level
4. Complete the task to earn coins!

### Building Your Galaxy
1. Complete tasks to accumulate coins
2. Navigate to "Your Galaxy" page
3. Use your coins to add new celestial objects
4. Watch your universe expand!

### Tracking Habits
1. Go to the Habits section
2. Create a new habit with your desired frequency
3. Check in daily to maintain your streak
4. Earn bonus coins for consistent habit tracking!

### Using AI Chat
1. Access the AI chat feature.
2. Discuss upcoming tasks and habits with the AI.
3. Use the AI to help define and determine the difficulty and duration of new items.
4. Track progress and improvements through your conversation.

### Reviewing Your Progress (Statistics Dashboard)
1. Navigate to the "Statistics Dashboard" or "Calendar Overview" section.
2. Review trends and patterns across your completed tasks and habits.
3. Check monthly performance, completion rates, and consistency to stay on track.
   
---

## 🛠️ Technology Stack

- **Backend**: Java 25, Spring Boot 4 (multi-module Maven project: `app`, `controller`, `service`, `repository`, `model`)
- **Frontend**: React 18 + TypeScript, Vite, Tailwind CSS, Radix UI
- **Database**: PostgreSQL
- **AI**: OpenAI API (chat, daily briefings, and habit coach notices)
- **Auth**: AWS Cognito
- **Hosting**: AWS Elastic Beanstalk (backend), AWS Amplify (frontend), AWS RDS (database), AWS S3 (assets)
- **Testing**: JUnit + Mockito (backend), Vitest + React Testing Library (frontend)

---

## 📋 Roadmap

- [x] Initial project setup
- [x] Core task management system
- [x] Habit tracking functionality
- [x] Coin reward mechanism
- [x] Galaxy visualization system
- [x] User authentication
- [x] Data persistence
- [x] AI chat, daily briefings, and habit coach notices
- [ ] Mobile responsiveness
- [ ] Additional galaxy customization options
- [ ] Social features (optional)

---

## 👥 Authors

- **Ofri K** - *creator* - [ofriK1708](https://github.com/ofriK1708)
- **Yael Moshkovish** - *creator* - [yaelmosh](https://github.com/yaelmosh)

---

<div align="center">

**Made with ❤️ and ☕**

*Transform your productivity journey into a cosmic adventure!* 🪐

</div>
