import { useState } from "react";
import { Login } from "./components/Login";
import { GalaxyView } from "./components/GalaxyView";
import { TaskDashboard } from "./components/TaskDashboard";
import { AIChat } from "./components/AIChat";
import { Shop } from "./components/Shop";
import { CheckCircle, AlertCircle, LogOut } from "lucide-react";
import { Statistics } from "./components/Statistics";
import { Profile } from "./components/Profile";
import { NavigationBar } from "./components/NavigationBar";
import { AddTaskModal } from "./components/AddTaskModal";
import { AddHabitModal } from "./components/AddHabitModal";
import { HabitTracker } from "./components/HabitTracker";
import confetti from 'canvas-confetti';
import { UserProvider, useUser } from "../context/UserContext.tsx";

type Screen = 'tasks' | 'habits' | 'galaxy' | 'chat' | 'profile' | 'shop' | 'statistics';

interface PlanetData {
  id: string;
  name: string;
  type: string;
  size: number;
  position: { x: number; y: number };
}

const REAL_PLANET_DATA: Record<string, { size: number, position: { x: number, y: number } }> = {
  sun: { size: 400, position: { x: 0, y: 50 } },
  mercury: { size: 24, position: { x: 18, y: 50 } },
  venus: { size: 36, position: { x: 28, y: 52 } },
  earth: { size: 40, position: { x: 40, y: 48 } },
  mars: { size: 30, position: { x: 52, y: 51 } },
  jupiter: { size: 90, position: { x: 68, y: 49 } },
  saturn: { size: 80, position: { x: 84, y: 51 } },
  neptune: { size: 50, position: { x: 95, y: 50 } },
};

function MainApp() {
  const { user, spendCoins } = useUser();

  const [localCoins, setLocalCoins] = useState(user?.totalCoins || 1250000);
  const coinBalance = localCoins;

  const [currentScreen, setCurrentScreen] = useState<Screen>('galaxy');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const triggerConfetti = () => {
    confetti({
      particleCount: 100,
      spread: 90,
      origin: { y: 0.6 },
      scalar: 2,
      gravity: 1.2,
      ticks: 150,
      colors: ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ec4899'],
    });
  };

  const [tasks, setTasks] = useState([
    { id: '1', title: 'Complete project proposal', description: 'Write and submit the Q1 project proposal for the new initiative', difficulty: 'hard' as const, completed: false, reward: 50, duration: '2 hours', dueDate: '2026-03-15' },
    { id: '2', title: 'Review pull requests', description: 'Check and approve pending code reviews', difficulty: 'medium' as const, completed: true, reward: 30, duration: '45 min', dueDate: '2026-03-15' },
    { id: '3', title: 'Daily standup meeting', description: 'Attend the team sync meeting at 10 AM', difficulty: 'easy' as const, completed: true, reward: 10, duration: '15 min', dueDate: '2026-03-15' },
    { id: '4', title: 'Update documentation', description: 'Add API documentation for new endpoints', difficulty: 'medium' as const, completed: false, reward: 25, duration: '1 hour', dueDate: '2026-03-15' },
    { id: '5', title: 'Client presentation', description: 'Present quarterly results to the client', difficulty: 'hard' as const, completed: false, reward: 60, duration: '3 hours', dueDate: '2026-03-15' },
    { id: '6', title: 'Respond to emails', description: 'Clear inbox and respond to pending emails', difficulty: 'easy' as const, completed: false, reward: 15, duration: '30 min', dueDate: '2026-03-15' },
  ]);

  const [habits, setHabits] = useState([
    { id: 'h1', title: 'Drink 2L of Water', streak: 3, completedToday: false, baseReward: 10 },
    { id: 'h2', title: 'Read 10 Pages', streak: 5, completedToday: true, baseReward: 15 },
    { id: 'h3', title: 'Daily Code Practice', streak: 0, completedToday: false, baseReward: 20 },
  ]);

  const [chatMessages, setChatMessages] = useState([
    { id: '1', type: 'ai' as const, content: 'Hello! I can help you manage your tasks. What would you like to work on today?', timestamp: new Date(Date.now() - 3600000) },
    { id: '2', type: 'user' as const, content: 'I need to focus on deep work today', timestamp: new Date(Date.now() - 3000000) },
    { id: '3', type: 'task-suggestion' as const, content: 'Great! Here\'s a challenging task that requires deep focus:', timestamp: new Date(Date.now() - 2900000), taskData: { title: 'Research and analysis for new feature', difficulty: 'hard' as const, reward: 75 } },
  ]);

  const [shopItems, setShopItems] = useState([
    { id: '1', name: 'The Sun', type: 'sun' as const, price: 2000, description: 'The blazing star at the center of our solar system. The ultimate achievement!', unlocked: false },
    { id: '2', name: 'Earth', type: 'earth' as const, price: 600, description: 'The blue planet with oceans and continents', unlocked: false },
    { id: '3', name: 'Jupiter', type: 'jupiter' as const, price: 800, description: 'The gas giant with iconic stripes', unlocked: false },
    { id: '4', name: 'Mars', type: 'mars' as const, price: 450, description: 'The red planet of mysteries', unlocked: false },
    { id: '5', name: 'Saturn', type: 'saturn' as const, price: 1000, description: 'The ringed beauty of the solar system', unlocked: false },
    { id: '6', name: 'Neptune', type: 'neptune' as const, price: 700, description: 'The deep blue ice giant', unlocked: false },
    { id: '7', name: 'Venus', type: 'venus' as const, price: 550, description: 'The bright morning star', unlocked: false },
    { id: '8', name: 'Mercury', type: 'mercury' as const, price: 400, description: 'The swift messenger planet', unlocked: false },
  ]);

  const [planets, setPlanets] = useState<PlanetData[]>([]);

  const userProfile = {
    name: user?.displayName || 'Explorer',
    email: user?.email || '',
    level: 12, xp: 2450, xpToNextLevel: 3000, tasksCompleted: 156,
    achievements: [
      { id: '1', name: 'First Task', icon: '🎯', unlocked: true }, { id: '2', name: 'Week Streak', icon: '🔥', unlocked: true },
      { id: '3', name: 'Deep Work', icon: '🧠', unlocked: true }, { id: '4', name: 'Speed Demon', icon: '⚡', unlocked: false },
      { id: '5', name: 'Consistent', icon: '📊', unlocked: true }, { id: '6', name: 'Early Bird', icon: '🌅', unlocked: false },
      { id: '7', name: 'Night Owl', icon: '🦉', unlocked: true }, { id: '8', name: 'Task Master', icon: '👑', unlocked: false },
    ],
  };

  const handleTaskToggle = (id: string) => {
    setTasks(tasks.map(task => {
      if (task.id === id) {
        const newCompleted = !task.completed;
        if (newCompleted) {
          setLocalCoins(prev => prev + task.reward);
          triggerConfetti();
          showToast(`Mission Accomplished! You earned ${task.reward} coins 🌟`, 'success');
        }
        else {
          setLocalCoins(prev => prev - task.reward);
        }
        return { ...task, completed: newCompleted };
      }
      return task;
    }));
  };

  const handleTaskDelete = (id: string) => { setTasks(tasks.filter(task => task.id !== id)); };

  const [isAddTaskModalOpen, setIsAddTaskModalOpen] = useState(false);
  const [isAddHabitModalOpen, setIsAddHabitModalOpen] = useState(false);

  const handleQuickAdd = () => { setIsAddTaskModalOpen(true); };

  const handleCreateNewTask = (taskData: any) => {
    const newTask = {
      id: Date.now().toString(), title: taskData.title, description: taskData.description || '',
      difficulty: taskData.difficulty, completed: false, reward: taskData.reward, duration: taskData.duration, dueDate: taskData.dueDate,
    };
    setTasks([newTask, ...tasks]);
  };

  const handleHabitCheck = (id: string) => {
    setHabits(habits.map(habit => {
      if (habit.id === id && !habit.completedToday) {
        const totalReward = habit.baseReward + (habit.streak * 2);
        setLocalCoins(prev => prev + totalReward);

        triggerConfetti();
        showToast(`Habit Logged! +${totalReward} coins added to your balance 💫`, 'success');

        return { ...habit, completedToday: true, streak: habit.streak + 1 };
      }
      return habit;
    }));
  };

  const handleOpenChat = () => { setCurrentScreen('chat'); };

  const handleSendMessage = (message: string) => {
    const newMessage = { id: Date.now().toString(), type: 'user' as const, content: message, timestamp: new Date() };
    setChatMessages([...chatMessages, newMessage]);
    setTimeout(() => {
      const aiResponse = { id: (Date.now() + 1).toString(), type: 'ai' as const, content: 'I understand! Let me help you with that. Would you like me to create a task for it?', timestamp: new Date() };
      setChatMessages(prev => [...prev, aiResponse]);
    }, 1000);
  };

  const handleAddTask = (taskData: any) => {
    const newTask = { id: Date.now().toString(), title: taskData.title, description: 'Created via AI chat',
      difficulty: taskData.difficulty, completed: false, reward: taskData.reward, duration: '30 min',
      dueDate: new Date().toISOString().split('T')[0],
    };
    setTasks([newTask, ...tasks]);
    showToast('Task added successfully!', 'success');
  };

  const handleAddHabit = (habitData: { title: string; baseReward: number }) => {
    const newHabit = {
      id: Date.now().toString(), title: habitData.title, streak: 0, completedToday: false, baseReward: habitData.baseReward,
    };
    setHabits([newHabit, ...habits]);
  };

  const handlePurchase = (itemId: string) => {
    const item = shopItems.filter(i => i.id === itemId)[0];
    if (item && coinBalance >= item.price && !item.unlocked) {
      setLocalCoins(prev => prev - item.price);
      spendCoins(item.price);

      setShopItems(shopItems.map(i => i.id === itemId ? { ...i, unlocked: true } : i));

      const pData = REAL_PLANET_DATA[item.type] || {
        size: Math.floor(Math.random() * 60) + 70,
        position: { x: Math.floor(Math.random() * 70) + 15, y: Math.floor(Math.random() * 70) + 15 }
      };

      const newPlanet = {
        id: Date.now().toString(),
        name: item.name,
        type: item.type,
        size: pData.size,
        position: pData.position
      };
      setPlanets([...planets, newPlanet]);

      showToast(`Successfully unlocked ${item.name}! It has been added to your Galaxy 🚀`, 'success');
    } else if (item && item.unlocked) {
      showToast("You already own this object!", 'error');
    } else {
      showToast("Not enough coins.", 'error');
    }
  };

  return (
      <div className="h-screen w-full flex flex-col bg-slate-900 overflow-hidden relative">

        {toast && (
            <div className="fixed top-6 left-1/2 transform -translate-x-1/2 z-[100] animate-in slide-in-from-top-4 fade-in duration-300">
              <div className={`flex items-center gap-3 px-5 py-3 rounded-2xl shadow-2xl border ${
                  toast.type === 'success'
                      ? 'bg-green-500/20 border-green-500/30 text-green-100'
                      : 'bg-red-500/20 border-red-500/30 text-red-100'
              } backdrop-blur-md`}>
                {toast.type === 'success' ? <CheckCircle className="w-5 h-5 text-green-400" /> : <AlertCircle className="w-5 h-5 text-red-400" />}
                <p className="font-medium text-sm tracking-wide">{toast.message}</p>
              </div>
            </div>
        )}

        {/* Main content area */}
        <div className="flex-1 overflow-hidden">
          {currentScreen === 'galaxy' && <GalaxyView coinBalance={coinBalance} planets={planets} />}
          {currentScreen === 'tasks' && <TaskDashboard tasks={tasks} onTaskToggle={handleTaskToggle} onQuickAdd={handleQuickAdd} onOpenChat={handleOpenChat} onTaskDelete={handleTaskDelete} />}
          {currentScreen === 'habits' && <HabitTracker habits={habits} onHabitCheck={handleHabitCheck} onAddHabitClick={() => setIsAddHabitModalOpen(true)} />}
          {currentScreen === 'chat' && <AIChat messages={chatMessages} onSendMessage={handleSendMessage} onAddTask={handleAddTask} />}
          {currentScreen === 'shop' && <Shop items={shopItems} coinBalance={coinBalance} onPurchase={handlePurchase} />}
          {currentScreen === 'statistics' && (
              <Statistics
                  tasks={tasks}
                  habits={habits}
                  totalCoinsEarned={coinBalance}
                  currentStreak={habits.reduce((max, h) => Math.max(max, h.streak), 0)}
              />
          )}
          {currentScreen === 'profile' && <Profile user={userProfile} coinBalance={coinBalance} />}
        </div>

        <AddTaskModal isOpen={isAddTaskModalOpen} onClose={() => setIsAddTaskModalOpen(false)} onAdd={handleCreateNewTask} />
        <AddHabitModal isOpen={isAddHabitModalOpen} onClose={() => setIsAddHabitModalOpen(false)} onAdd={handleAddHabit} />

        <NavigationBar currentScreen={currentScreen} onNavigate={setCurrentScreen} />
      </div>
  );
}

function AppWrapper() {
  const { user, isLoading } = useUser();

  if (isLoading) {
    return (
        <div className="h-screen w-full flex flex-col items-center justify-center bg-slate-950 text-white">
          <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mb-4" />
          <h2 className="text-xl font-bold animate-pulse">Loading Galaxy...</h2>
        </div>
    );
  }

  if (!user) {
    return <Login />;
  }
  return <MainApp />;
}

export default function App() {
  return (
      <UserProvider>
        <AppWrapper />
      </UserProvider>
  );
}