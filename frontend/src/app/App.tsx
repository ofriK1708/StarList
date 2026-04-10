import { useState, useEffect } from "react";
import { Login } from "./components/Login";
import { GalaxyView } from "./components/GalaxyView";
import { TaskDashboard } from "./components/TaskDashboard";
import { AIChat } from "./components/AIChat";
import { Shop } from "./components/Shop";
import { CheckCircle, AlertCircle } from "lucide-react";
import { Statistics } from "./components/Statistics";
import { Profile } from "./components/Profile";
import { NavigationBar } from "./components/NavigationBar";
import { AddTaskModal } from "./components/AddTaskModal";
import { AddHabitModal } from "./components/AddHabitModal";
import { EditHabitModal } from "./components/EditHabitModal";
import { HabitTracker } from "./components/HabitTracker";
import confetti from 'canvas-confetti';
import { UserProvider, useUser } from "../context/UserContext";
import { tasksApi, TaskResponse, AddTaskRequest } from "../services/taskApi";
import { habitsApi, HabitResponse, AddHabitRequest, UpdateHabitRequest } from "../services/habitsApi";
import { Amplify } from 'aws-amplify';
import { Authenticator } from '@aws-amplify/ui-react';
import '@aws-amplify/ui-react/styles.css';

import { awsConfig } from '../aws-config';

Amplify.configure(awsConfig);

type Screen = 'tasks' | 'habits' | 'galaxy' | 'chat' | 'profile' | 'shop' | 'statistics';

interface PlanetData {
  id: string;
  name: string;
  type: string;
  size: number;
  position: { x: number; y: number };
}

interface ChatMessage {
  id: string;
  type: 'ai' | 'user' | 'task-suggestion';
  content: string;
  timestamp: Date;
  taskData?: any;
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

  const [localCoins, setLocalCoins] = useState(user?.totalCoins || 0);
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

  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [habits, setHabits] = useState<HabitResponse[]>([]);

  useEffect(() => {
    const loadData = async () => {
      try {
        const [fetchedTasks, fetchedHabits] = await Promise.all([
          tasksApi.getTasks(),
          habitsApi.getHabits()
        ]);
        setTasks(fetchedTasks);
        setHabits(fetchedHabits);
      } catch (error) {
        console.error("Failed to load data:", error);
        showToast("Failed to load data from the server", "error");
      }
    };
    loadData();
  }, []);

  const handleTaskToggle = async (taskId: number) => {
    const task = tasks.find(t => t.taskId === taskId);
    if (!task) return;

    if (task.status === 'COMPLETED') {
      showToast("Mission is already completed!", "success");
      return;
    }

    try {
      const response = await tasksApi.completeTask(taskId);
      setTasks(tasks.map(t => t.taskId === taskId ? { ...t, status: 'COMPLETED' } : t));
      setLocalCoins(response.newTotalCoins);
      triggerConfetti();
      showToast(`Mission Accomplished! You earned ${response.coinsEarned} coins 🌟`, 'success');
    } catch (error) {
      showToast("Failed to complete mission. Try again.", "error");
    }
  };

  const handleTaskDelete = async (taskId: number) => {
    try {
      await tasksApi.deleteTask(taskId);
      setTasks(tasks.filter(task => task.taskId !== taskId));
      showToast("Mission deleted.", "success");
    } catch (error) {
      showToast("Failed to delete mission.", "error");
    }
  };

  const handleCreateNewTask = async (taskData: AddTaskRequest) => {
    try {
      const newTask = await tasksApi.createTask(taskData);
      setTasks([newTask, ...tasks]);
      showToast('Mission Added successfully!', 'success');
      setIsAddTaskModalOpen(false);
    } catch (error) {
      showToast("Failed to add mission.", "error");
    }
  };

  const [isEditHabitModalOpen, setIsEditHabitModalOpen] = useState(false);
  const [habitToEdit, setHabitToEdit] = useState<HabitResponse | null>(null);

  const handleHabitCheck = async (habitId: number) => {
    try {
      const response = await habitsApi.completeHabit(habitId);
      setLocalCoins(response.newTotalCoins);
      setHabits(habits.map(habit => {
        if (habit.habitId === habitId) {
          const today = new Date().toISOString().split('T')[0];
          return {
            ...habit,
            currentStreak: response.currentStreak,
            bestStreak: response.bestStreak,
            lastCompletedDate: today,
            totalCompletions: habit.totalCompletions + 1
          };
        }
        return habit;
      }));
      triggerConfetti();
      showToast(`Habit Logged! +${response.coinsEarned} coins 💫`, 'success');
    } catch (error: any) {
      if (error.response?.status === 409) {
        showToast("You already completed this habit today!", "error");
      } else {
        showToast("Failed to log habit. Try again.", "error");
      }
    }
  };

  const handleAddHabit = async (habitData: AddHabitRequest) => {
    try {
      const newHabit = await habitsApi.createHabit(habitData);
      setHabits([newHabit, ...habits]);
      showToast('Habit added successfully!', 'success');
      setIsAddHabitModalOpen(false);
    } catch (error) {
      showToast("Failed to add habit.", "error");
    }
  };

  const handleUpdateHabit = async (habitId: number, data: UpdateHabitRequest) => {
    try {
      const updatedHabit = await habitsApi.updateHabit(habitId, data);
      setHabits(habits.map(habit =>
          habit.habitId === habitId ? { ...habit, ...updatedHabit } : habit
      ));
      showToast('Habit updated successfully!', 'success');
      setIsEditHabitModalOpen(false);
      setHabitToEdit(null);
    } catch (error) {
      showToast("Failed to update habit.", "error");
    }
  };

  const handleDeleteHabit = async (habitId: number) => {
    try {
      await habitsApi.deleteHabit(habitId);
      setHabits(habits.filter(habit => habit.habitId !== habitId));
      showToast("Habit deleted.", "success");
    } catch (error) {
      showToast("Failed to delete habit.", "error");
    }
  };

  const openEditHabitModal = (habit: HabitResponse) => {
    setHabitToEdit(habit);
    setIsEditHabitModalOpen(true);
  };

  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    { id: '1', type: 'ai', content: 'Hello! I can help you manage your tasks. What would you like to work on today?', timestamp: new Date(Date.now() - 3600000) },
  ]);

  const [shopItems, setShopItems] = useState([
    { id: '1', name: 'The Sun', type: 'sun' as const, price: 2000, description: 'The blazing star at the center of our solar system.', unlocked: false },
    { id: '2', name: 'Earth', type: 'earth' as const, price: 600, description: 'The blue planet with oceans and continents', unlocked: false },
    { id: '3', name: 'Jupiter', type: 'jupiter' as const, price: 800, description: 'The gas giant with iconic stripes', unlocked: false },
  ]);

  const [planets, setPlanets] = useState<PlanetData[]>([]);

  const userProfile = {
    name: user?.displayName || 'Explorer',
    email: user?.email || '',
    level: 1, xp: 0, xpToNextLevel: 1000, tasksCompleted: tasks.filter(t => t.status === 'COMPLETED').length,
    achievements: [
      { id: '1', name: 'First Task', icon: '🎯', unlocked: tasks.some(t => t.status === 'COMPLETED') },
      { id: '2', name: 'Space Explorer', icon: '🚀', unlocked: true },
    ],
  };

  const [isAddTaskModalOpen, setIsAddTaskModalOpen] = useState(false);
  const [isAddHabitModalOpen, setIsAddHabitModalOpen] = useState(false);

  const handleQuickAdd = () => { setIsAddTaskModalOpen(true); };
  const handleOpenChat = () => { setCurrentScreen('chat'); };

  const handleSendMessage = (message: string) => {
    const newMessage: ChatMessage = { id: Date.now().toString(), type: 'user', content: message, timestamp: new Date() };
    setChatMessages([...chatMessages, newMessage]);
    setTimeout(() => {
      const aiResponse: ChatMessage = { id: (Date.now() + 1).toString(), type: 'ai', content: 'I understand! Let me help you with that.', timestamp: new Date() };
      setChatMessages(prev => [...prev, aiResponse]);
    }, 1000);
  };

  const handleAddTaskFromChat = async (taskData: any) => {
    try {
      const newTask = await tasksApi.createTask({
        title: taskData.title,
        description: 'Created via AI chat',
        difficultyLevel: taskData.difficulty.toUpperCase() || 'MEDIUM',
        durationMinutes: 30
      });
      setTasks([newTask, ...tasks]);
      showToast('Task added successfully via AI!', 'success');
    } catch (error) {
      showToast('Failed to add AI task.', 'error');
    }
  };

  const handlePurchase = (itemId: string) => {
    const item = shopItems.find(i => i.id === itemId);
    if (item && coinBalance >= item.price && !item.unlocked) {
      setLocalCoins(prev => prev - item.price);
      spendCoins(item.price);
      setShopItems(shopItems.map(i => i.id === itemId ? { ...i, unlocked: true } : i));
      const pData = REAL_PLANET_DATA[item.type] || {
        size: Math.floor(Math.random() * 60) + 70,
        position: { x: Math.floor(Math.random() * 70) + 15, y: Math.floor(Math.random() * 70) + 15 }
      };
      const newPlanet = { id: Date.now().toString(), name: item.name, type: item.type, size: pData.size, position: pData.position };
      setPlanets([...planets, newPlanet]);
      showToast(`Successfully unlocked ${item.name}! 🚀`, 'success');
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

        <div className="flex-1 overflow-hidden">
          {currentScreen === 'galaxy' && <GalaxyView coinBalance={coinBalance} planets={planets} />}
          {currentScreen === 'tasks' && <TaskDashboard tasks={tasks} onTaskToggle={handleTaskToggle} onQuickAdd={handleQuickAdd} onOpenChat={handleOpenChat} onTaskDelete={handleTaskDelete} />}
          {currentScreen === 'habits' && (
              <HabitTracker
                  habits={habits}
                  onHabitCheck={handleHabitCheck}
                  onAddHabitClick={() => setIsAddHabitModalOpen(true)}
                  onEditHabitClick={openEditHabitModal}
                  onDeleteHabit={handleDeleteHabit}
              />
          )}
          {currentScreen === 'chat' && <AIChat messages={chatMessages} onSendMessage={handleSendMessage} onAddTask={handleAddTaskFromChat} />}
          {currentScreen === 'shop' && <Shop items={shopItems} coinBalance={coinBalance} onPurchase={handlePurchase} />}
          {currentScreen === 'statistics' && (
              <Statistics
                  tasks={tasks as any}
                  habits={habits}
                  totalCoinsEarned={coinBalance}
                  currentStreak={habits.reduce((max, h) => Math.max(max, h.currentStreak), 0)}
              />
          )}
          {currentScreen === 'profile' && <Profile user={userProfile} coinBalance={coinBalance} />}
        </div>

        <AddTaskModal isOpen={isAddTaskModalOpen} onClose={() => setIsAddTaskModalOpen(false)} onAdd={handleCreateNewTask} />
        <AddHabitModal isOpen={isAddHabitModalOpen} onClose={() => setIsAddHabitModalOpen(false)} onAdd={handleAddHabit} />
        <EditHabitModal
            isOpen={isEditHabitModalOpen}
            onClose={() => { setIsEditHabitModalOpen(false); setHabitToEdit(null); }}
            habitToEdit={habitToEdit}
            onUpdate={handleUpdateHabit}
        />

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
  if (!user) return <Login />;
  return <MainApp />;
}

export default function App() {
  return (
      <UserProvider>
        <AppWrapper />
      </UserProvider>
  );
}